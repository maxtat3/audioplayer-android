package edu.sintez.audioplayer.app.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;

import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;
import edu.sintez.audioplayer.app.activity.MainActivity;
import edu.sintez.audioplayer.R;
import edu.sintez.audioplayer.app.audiofocus.AudioFocusHelper;
import edu.sintez.audioplayer.app.audiofocus.MusicFocusable;
import edu.sintez.audioplayer.app.model.Track;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Service that handles audio track playback. Audio track pass from {@link MainActivity}.
 */
public class MusicService extends Service implements OnCompletionListener,
		OnPreparedListener, OnErrorListener, MusicFocusable {

    // The tag we put on debug messages
    private static final String LOG = MusicService.class.getName();

	// Displayed log in terminal. If true - displaying log.
    private boolean isDebug = true;

    // These are the Intent actions that service are handled.
    public static final String ACTION_TOGGLE_PLAYBACK = "edu.sintez.audioplayer.app.action.TOGGLE_PLAYBACK";
    public static final String ACTION_PLAY = "edu.sintez.audioplayer.app.action.PLAY";
    public static final String ACTION_PAUSE = "edu.sintez.audioplayer.app.action.PAUSE";
    public static final String ACTION_JUMP_TO = "edu.sintez.audioplayer.app.action.JUMP_TO";
    public static final String ACTION_STOP = "edu.sintez.audioplayer.app.action.STOP";

	public static final String TRACK_TIME_KEY = MusicService.class.getName() + "." + "TrackTime";
	public static final String PLAY_TIME_CURRENT = MusicService.class.getName() + "." + "CurrentTrackPlayingTime";
	public static final String PLAY_TIME_ALL = MusicService.class.getName() + "." + "AllTrackPlayTime";
	public static final String GET_NEXT_TRACK_KEY = MusicService.class.getName() + "." + "GetNextTrack";

	/**
	 * Indicates the state our service:
	 */
	private enum State {
		STOPPED,        // media player is stopped and not prepared to play
		PREPARING,      // media player is preparing ...
		PLAYING,        // playback active (media player ready!). (but the media player may actually be
						// paused in this state if we don't have audio focus. But we stay in this state
						// so that we know we have to resume playback once we get focus back)
		PAUSED          // playback paused (media player ready !)
	}
	private State state = State.STOPPED;

	/**
	 * Do we have audio focus ?
	 */
	private enum AudioFocus {
		NO_FOCUS_NO_DUCK,   // app don't have audio focus, and can't duck
		NO_FOCUS_CAN_DUCK,  // app don't have focus, but can play at a low volume ("ducking")
		FOCUSED             // app have full audio focus
	}
	private AudioFocus audioFocus = AudioFocus.NO_FOCUS_NO_DUCK;

	/**
	 * Class to deal with audio focus.
	 * May be used on sdk level 8 and above.
	 * If not available, this will be null. Always check for null before using!
	 */
	private AudioFocusHelper audioFocusHelper = null;

	/**
	 * The volume we set the media player to when we lose audio focus,
	 * but are allowed to reduce the volume instead of stopping playback.
	 *
	 * @see MediaPlayer
	 */
    public static final float DUCK_VOLUME = 0.3f;

	/**
	 * Main audio player object.
	 */
	private MediaPlayer mp = null;

	/**
	 * Title of the song we are currently playing using for notification
	 */
	private String songTitle = "";

	/**
	 * The ID we use for the notification
	 */
	private final int NOTIFICATION_ID = 1;

	/**
	 * For displayed notifications
	 */
    private NotificationManager notificationManager;

    private Notification.Builder mNotificationBuilder = null;


	@Override
	public void onCreate() {
		if (isDebug) Log.d(LOG, "Creating service");

		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		audioFocusHelper = new AudioFocusHelper(getApplicationContext(), this);
	}

	private Track track;

	/**
	 * Called when we receive an Intent. When we receive an intent sent to us via startService(),
	 * this is the method that gets called. So here we react appropriately depending on the
	 * Intent's action, which specifies what is being requested of us.
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String action = intent.getAction();

		if (action.equals(ACTION_TOGGLE_PLAYBACK)) togglePlaybackRequest();
		else if (action.equals(ACTION_PLAY)) playRequest(intent);
		else if (action.equals(ACTION_PAUSE)) pauseRequest();
		else if (action.equals(ACTION_JUMP_TO)) jumpInsideTrackRequest(intent);
		else if (action.equals(ACTION_STOP)) stopRequest();

		return START_NOT_STICKY; // Means we started the service, but don't want it to
		// restart in case it's killed.
	}

	private void togglePlaybackRequest() {
		if (state == State.PAUSED || state == State.STOPPED) {
			prepareToPlay();
		} else {
			pauseRequest();
		}
	}

	/**
	 * Getting audio data and call {@link #prepareToPlay()} method.
	 *
	 * @param intent contained audio track data
	 */
	private void playRequest(Intent intent) {
		Bundle bundle = intent.getExtras();
		if (bundle != null && bundle.containsKey(MainActivity.SERVICE_PLAYING_TRACK_KEY)) {
			track = bundle.getParcelable(MainActivity.SERVICE_PLAYING_TRACK_KEY);
		}
		if (state == State.PLAYING || state == State.PAUSED) {
			tryToGetAudioFocus();
			playNextSong();
		}
		prepareToPlay();
	}

	private void pauseRequest() {
		if (isDebug) Log.d(LOG, "pauseRequest");

		if (state == State.PLAYING) {
			// Pause media player and cancel the 'foreground service' state.
			state = State.PAUSED;
			mp.pause();
			relaxResources(false); // while paused, we always retain the MediaPlayer
			// do not give up audio focus
		}
	}

	private void jumpInsideTrackRequest(Intent intent) {
		int jumpTo = intent.getIntExtra(MainActivity.SERVICE_JUMP_TO_POS_KEY, 0);
		if (state == State.PLAYING || state == State.PAUSED) {
			mp.seekTo(jumpTo);
		}
	}

	/**
	 * Stop playing audio track
	 */
	private void stopRequest() {
		stopRequest(false);
	}

	private void stopRequest(boolean force) {
		if (isDebug) Log.d(LOG, "stopRequest, force = " + force);
		if (state == State.PLAYING || state == State.PAUSED || force) {
			state = State.STOPPED;

			// let go of all resources...
			relaxResources(true);
			giveUpAudioFocus();

			// service is no longer necessary. Will be started again if needed.
			stopSelf();
		}
	}

    private void prepareToPlay() {
        if (isDebug) Log.d(LOG, "playRequest");

        tryToGetAudioFocus();

        // actually play the song
        if (state == State.STOPPED) {
            // If we're stopped, just go ahead to the next song and start playing
            playNextSong();
        }
        else if (state == State.PAUSED) {
            // If we're paused, just continue playback and restore the 'foreground service' state.
            state = State.PLAYING;
            setUpAsForeground(songTitle + " (playing)");
            configAndStartMediaPlayer();
        }
    }

	/**
	 * Makes sure the media player exists and has been reset. This will create the media player
	 * if needed, or reset the existing media player if one already exists.
	 */
	private void createMediaPlayerIfNeeded() {
		if (mp == null) {
			if (isDebug) Log.d(LOG, "createMediaPlayerIfNeeded - mp is null !");

			mp = new MediaPlayer();

			// Make sure the media player will acquire a wake-lock while playing. If we don't do
			// that, the CPU might go to sleep while the song is playing, causing playback to stop.
			//
			// Remember that to use this, we have to declare the android.permission.WAKE_LOCK
			// permission in AndroidManifest.xml.
			mp.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

			// we want the media player to notify us when it's ready preparing, and when it's done playing:
			mp.setOnPreparedListener(this); // ready playback
			mp.setOnCompletionListener(this); // end playback listener
			mp.setOnErrorListener(this);
		} else {
			if (isDebug) Log.d(LOG, "createMediaPlayerIfNeeded - mp is NOT null");
			mp.reset();
		}
	}

	/**
	 * Releases resources used by the service for playback. This includes the "foreground service"
	 * status and notification, the wake locks and possibly the MediaPlayer.
	 *
	 * @param releaseMediaPlayer Indicates whether the Media Player should also be released or not
	 */
	private void relaxResources(boolean releaseMediaPlayer) {
		if (isDebug) Log.d(LOG, "relaxResources");
		// stop being a foreground service
		stopForeground(true);

		// stop and release the Media Player, if it's available
		if (releaseMediaPlayer && mp != null) {
			mp.reset();
			mp.release();
			mp = null;
		}
	}

	/**
	 * Starts playing the next track. If manualUrl is null, the next track will be playing from
	 * local device storage. This track received from MainActivity from intent. If manualUrl is non-null,
	 * then it specifies the URL or path to the song that will be played next.
	 */
	private void playNextSong() {
		if (isDebug) Log.d(LOG, "playNextSong");
		state = State.STOPPED;
		relaxResources(false); // release everything except MediaPlayer

		try {
			Track playingItem = this.track;

			if (isDebug) Log.d(LOG, playingItem == null ? "playingItem is null ! " : "playingItem is not null." );
			if (playingItem == null) {
				Toast.makeText(
					this,
					"No available music to play. Place some music on your external storage "
						+ "device (e.g. your SD card) and try again.",
					Toast.LENGTH_LONG
				).show();
				stopRequest(true); // stop everything!
				return;
			}

			// set the source of the media player a a content URI
			createMediaPlayerIfNeeded();
			mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mp.setDataSource(getApplicationContext(), playingItem.getURI());

			songTitle = playingItem.getTitle();

			state = State.PREPARING;
			setUpAsForeground(songTitle + " (loading)");

			// starts preparing the media player in the background. When it's done, it will call
			// OnPreparedListener (that is, the onPrepared() method on this class, since we set
			// the listener to 'this').
			//
			// Until the media player is prepared, we *cannot* call start() on it!
			mp.prepareAsync();
		}
		catch (IOException ex) {
			if (isDebug) Log.e("MusicService", "IOException playing next song: " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	/**
	 * Reconfigures MediaPlayer according to audio focus settings and starts/restarts it. This
	 * method starts/restarts the MediaPlayer respecting the current audio focus state. So if
	 * we have focus, it will play normally; if we don't have focus, it will either leave the
	 * MediaPlayer paused or set it to a low volume, depending on what is allowed by the
	 * current focus settings. This method assumes mp != null, so if you are calling it,
	 * you have to do so from a context where you are sure this is the case.
	 */
	private void configAndStartMediaPlayer() {
		if (isDebug) Log.d(LOG, "configAndStartMediaPlayer");
		if (audioFocus == AudioFocus.NO_FOCUS_NO_DUCK) {
			// If we don't have audio focus and can't duck, we have to pause, even if state
			// is State.PLAYING. But we stay in the PLAYING state so that we know we have to resume
			// playback once we get the focus back.
			if (mp.isPlaying()) mp.pause();
			return;
		}
		else if (audioFocus == AudioFocus.NO_FOCUS_CAN_DUCK)
			mp.setVolume(DUCK_VOLUME, DUCK_VOLUME);  // playing in low volume
		else
			mp.setVolume(1.0f, 1.0f); // can be loud playing

		if (!mp.isPlaying()) mp.start();

		updatePlayingTime();
	}

	/**
	 * Configures service as a foreground service. A foreground service is a service that's doing
	 * something the user is actively aware of (such as playing music), and must appear to the
	 * user as a notification. That's why we create the notification here.
	 */
	private void setUpAsForeground(String text) {
		if (isDebug) Log.d(LOG, "setUpAsForeground");

		PendingIntent pi = PendingIntent.getActivity(
			getApplicationContext(),
			0,
			new Intent(getApplicationContext(), MainActivity.class),
			PendingIntent.FLAG_UPDATE_CURRENT
		);

		// Build the notification object.
		mNotificationBuilder = new Notification.Builder(getApplicationContext())
			.setSmallIcon(R.drawable.ic_stat_playing)
			.setTicker(text)
			.setWhen(System.currentTimeMillis())
			.setContentTitle("Music player")
			.setContentText(text)
			.setContentIntent(pi)
			.setOngoing(true);

		startForeground(NOTIFICATION_ID, mNotificationBuilder.build());
	}

	/**
	 * Updates the notification
	 *
	 * @param text notification text
	 */
	private void updateNotification(String text) {
		PendingIntent pi = PendingIntent.getActivity(
			getApplicationContext(),
			0,
			new Intent(getApplicationContext(), MainActivity.class),
			PendingIntent.FLAG_UPDATE_CURRENT
		);
		mNotificationBuilder.setContentText(text).setContentIntent(pi);
		notificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
	}

    /**
     * Called when media player is done playing current song.
     * When media player finished playing the current track, so go ahead and start the next.
     */
    @Override
    public void onCompletion(MediaPlayer player) {
        if (isDebug) Log.d(LOG, "onCompletion");
	    requestToGetNextTrack();
    }

    /**
     * Called when media player is done preparing.
     */
    @Override
    public void onPrepared(MediaPlayer player) {
        if (isDebug) Log.d(LOG, "onPrepared");
        // The media player is done preparing. That means we can start playing!
        state = State.PLAYING;
        updateNotification(songTitle + " (playing)");
        configAndStartMediaPlayer();
    }

	/**
	 * Отдаем аудио фокус системе
	 */
	private void giveUpAudioFocus() {
		if (isDebug) Log.d(LOG, "giveUpAudioFocus");
		if (audioFocus == AudioFocus.FOCUSED && audioFocusHelper != null && audioFocusHelper.abandonFocus())
			audioFocus = AudioFocus.NO_FOCUS_NO_DUCK;
	}

	/**
	 * Поптытка плеера получить аудио фокус
	 */
	private void tryToGetAudioFocus() {
		if (isDebug) Log.d(LOG, "tryToGetAudioFocus");
		if (audioFocus != AudioFocus.FOCUSED && audioFocusHelper != null
			&& audioFocusHelper.requestFocus())
			audioFocus = AudioFocus.FOCUSED;
	}

    @Override
    public void onGainedAudioFocus() {
        if (isDebug) Log.d(LOG, "onGainedAudioFocus");
        Toast.makeText(getApplicationContext(), "gained audio focus.", Toast.LENGTH_SHORT).show();
        audioFocus = AudioFocus.FOCUSED;

        // restart media player with new focus settings
        if (state == State.PLAYING)
            configAndStartMediaPlayer();
    }

    @Override
    public void onLostAudioFocus(boolean canDuck) {
        if (isDebug) Log.d(LOG, "onLostAudioFocus");
        Toast.makeText(
	        getApplicationContext(),
	        "lost audio focus." + (canDuck ? "can duck" : "no duck"),
	        Toast.LENGTH_SHORT
        ).show();
        audioFocus = canDuck ? AudioFocus.NO_FOCUS_CAN_DUCK : AudioFocus.NO_FOCUS_NO_DUCK;

        // start/restart/pause media player with new focus settings
        if (mp != null && mp.isPlaying()) configAndStartMediaPlayer();
    }

	/**
	 * Called when there's an error playing media. When this happens, the media player goes to
	 * the Error state. We warn the user about the error and reset the media player.
	 */
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		Toast.makeText(getApplicationContext(), "Media player error! Resetting.", Toast.LENGTH_SHORT).show();
		if (isDebug) Log.e(LOG, "Error: what=" + String.valueOf(what) + ", extra=" + String.valueOf(extra));

		state = State.STOPPED;
		relaxResources(true);
		giveUpAudioFocus();
		return true; // true indicates we handled the error
	}


    @Override
    public void onDestroy() {
        // Service is being killed, so make sure we release our resources
        state = State.STOPPED;
        relaxResources(true);
        giveUpAudioFocus();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

	/**
	 * Updating current and general time playing track.
	 * In this at 1 second interval get time position from media player.
	 *
	 * @see MediaPlayer
	 */
	private void updatePlayingTime() {
		final Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if (mp != null && mp.isPlaying()) {
					sendTime(mp.getCurrentPosition(), mp.getDuration());
				} else {
					timer.cancel();
					timer.purge();
				}
			}
		}, 0, 1000);
	}

	/**
	 * When audio track is playing, sending information to UI about current playing time
	 * and general playing time track.
	 *
	 * @param currentTime current position playing time in ms
	 * @param allTime general playing time track in ms
	 */
	private void sendTime(int currentTime, int allTime) {
		Intent i = new Intent(TRACK_TIME_KEY);
		i.putExtra(PLAY_TIME_CURRENT, currentTime);
		i.putExtra(PLAY_TIME_ALL, allTime);
		LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
	}

	/**
	 * Request to getting play next audio track in playlist
	 */
	private void requestToGetNextTrack() {
		Intent i = new Intent(GET_NEXT_TRACK_KEY);
		LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
	}
}
