package edu.sintez.audioplayer.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;
import edu.sintez.audioplayer.MainActivity;
import edu.sintez.audioplayer.retriever.MusicRetriever;
import edu.sintez.audioplayer.retriever.PrepareMusicRetrieverTask;
import edu.sintez.audioplayer.R;
import edu.sintez.audioplayer.audiofocus.AudioFocusHelper;
import edu.sintez.audioplayer.audiofocus.MusicFocusable;

import java.io.IOException;

/**
 * Service that handles media playback. This is the Service through which we perform all the media
 * handling in our application. Upon initialization, it starts a {@link MusicRetriever} to scan
 * the user's media. Then, it waits for Intents (which come from our main activity,
 * {@link MainActivity}, which signal the service to perform specific operations: Play, Pause,
 * Rewind, Skip, etc.
 */
public class MusicService extends Service implements OnCompletionListener, OnPreparedListener,
                OnErrorListener, MusicFocusable,
        PrepareMusicRetrieverTask.MusicRetrieverPreparedListener {

    // The tag we put on debug messages
    private static final String LOG = MusicService.class.getName();

    // These are the Intent actions that we are prepared to handle. Notice that the fact these
    // constants exist in our class is a mere convenience: what really defines the actions our
    // service can handle are the <action> tags in the <intent-filters> tag for our service in
    // AndroidManifest.xml.
    public static final String ACTION_TOGGLE_PLAYBACK =
            "com.example.android.musicplayer.action.TOGGLE_PLAYBACK";
    public static final String ACTION_PLAY = "com.example.android.musicplayer.action.PLAY";
    public static final String ACTION_PAUSE = "com.example.android.musicplayer.action.PAUSE";
    public static final String ACTION_STOP = "com.example.android.musicplayer.action.STOP";
    public static final String ACTION_SKIP = "com.example.android.musicplayer.action.SKIP";
    public static final String ACTION_REWIND = "com.example.android.musicplayer.action.REWIND";
    public static final String ACTION_URL = "com.example.android.musicplayer.action.URL";

    // The volume we set the media player to when we lose audio focus, but are allowed to reduce
    // the volume instead of stopping playback.
    public static final float DUCK_VOLUME = 0.3f;

    // our media player
    MediaPlayer mp = null;

    // our AudioFocusHelper object, if it's available (it's available on SDK level >= 8)
    // If not available, this will be null. Always check for null before using!
    AudioFocusHelper mAudioFocusHelper = null;

    // indicates the state our service:
    enum State {
        Retrieving, // the MediaRetriever is retrieving music
        Stopped,    // media player is stopped and not prepared to play
        Preparing,  // media player is preparing...
        Playing,    // playback active (media player ready!). (but the media player may actually be
                    // paused in this state if we don't have audio focus. But we stay in this state
                    // so that we know we have to resume playback once we get focus back)
        Paused      // playback paused (media player ready!)
    };

    State state = State.Retrieving;

    // if in Retrieving mode, this flag indicates whether we should start playing immediately
    // when we are ready or not.
    boolean mStartPlayingAfterRetrieve = false;

    // if mStartPlayingAfterRetrieve is true, this variable indicates the URL that we should
    // start playing when we are ready. If null, we should play a random song from the device
    Uri mWhatToPlayAfterRetrieve = null;

    // do we have audio focus?
    enum AudioFocus {
        NoFocusNoDuck,    // we don't have audio focus, and can't duck
        NoFocusCanDuck,   // we don't have focus, but can play at a low volume ("ducking")
        Focused           // we have full audio focus
    }
    AudioFocus audioFocus = AudioFocus.NoFocusNoDuck;

    // title of the song we are currently playing
	// using for notification
    String songTitle = "";

    // whether the song we are playing is streaming from the network
    boolean isStreaming = false;

    // Wifi lock that we hold when streaming files from the internet, in order to prevent the
    // device from shutting off the Wifi radio
    WifiLock wifiLock;

    // The ID we use for the notification (the onscreen alert that appears at the notification
    // area at the top of the screen as an icon -- and as text as well if the user expands the
    // notification area).
    final int NOTIFICATION_ID = 1;

    // Our instance of our MusicRetriever, which handles scanning for media and
    // providing titles and URIs as we need.
    MusicRetriever mRetriever;

    // our RemoteControlClient object, which will use remote control APIs available in
    // SDK level >= 14, if they're available.
//    RemoteControlClientCompat mRemoteControlClientCompat;

    // Dummy album art we will pass to the remote control (if the APIs are available).
    Bitmap mDummyAlbumArt;

    AudioManager mAudioManager;
    NotificationManager mNotificationManager;

    Notification.Builder mNotificationBuilder = null;

    /**
     * Makes sure the media player exists and has been reset. This will create the media player
     * if needed, or reset the existing media player if one already exists.
     */
    void createMediaPlayerIfNeeded() {
        if (mp == null) {
            mp = new MediaPlayer();

            // Make sure the media player will acquire a wake-lock while playing. If we don't do
            // that, the CPU might go to sleep while the song is playing, causing playback to stop.
            //
            // Remember that to use this, we have to declare the android.permission.WAKE_LOCK
            // permission in AndroidManifest.xml.
            mp.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

            // we want the media player to notify us when it's ready preparing, and when it's done
            // playing:
            mp.setOnPreparedListener(this); // ready playback
            mp.setOnCompletionListener(this); // end playback listener
            mp.setOnErrorListener(this);
        }
        else
            mp.reset();
    }

    @Override
    public void onCreate() {
        Log.d(LOG, "Creating service");

        // Create the Wifi lock (this does not acquire the lock, this just creates it)
        wifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                        .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        // Create the retriever and start an asynchronous task that will prepare it.
        mRetriever = new MusicRetriever(getContentResolver());
        (new PrepareMusicRetrieverTask(mRetriever,this)).execute();

        // create the Audio Focus Helper, if the Audio Focus feature is available (SDK 8 or above)
        if (android.os.Build.VERSION.SDK_INT >= 8)
            mAudioFocusHelper = new AudioFocusHelper(getApplicationContext(), this);
        else
            audioFocus = AudioFocus.Focused; // no focus feature, so we always "have" audio focus

        mDummyAlbumArt = BitmapFactory.decodeResource(getResources(), R.drawable.dummy_album_art);

    }

    /**
     * Called when we receive an Intent. When we receive an intent sent to us via startService(),
     * this is the method that gets called. So here we react appropriately depending on the
     * Intent's action, which specifies what is being requested of us.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action.equals(ACTION_TOGGLE_PLAYBACK)) processTogglePlaybackRequest();
        else if (action.equals(ACTION_PLAY)) processPlayRequest();
        else if (action.equals(ACTION_PAUSE)) processPauseRequest();
        else if (action.equals(ACTION_SKIP)) processSkipRequest();
        else if (action.equals(ACTION_STOP)) processStopRequest();
        else if (action.equals(ACTION_REWIND)) processRewindRequest();
        else if (action.equals(ACTION_URL)) processAddRequest(intent);

        return START_NOT_STICKY; // Means we started the service, but don't want it to
                                 // restart in case it's killed.
    }

    void processTogglePlaybackRequest() {
        if (state == State.Paused || state == State.Stopped) {
            processPlayRequest();
        } else {
            processPauseRequest();
        }
    }

    void processPlayRequest() {
        if (state == State.Retrieving) {
            // If we are still retrieving media, just set the flag to start playing when we're
            // ready
            mWhatToPlayAfterRetrieve = null; // play a random song
            mStartPlayingAfterRetrieve = true;
            return;
        }

        tryToGetAudioFocus();

        // actually play the song

        if (state == State.Stopped) {
            // If we're stopped, just go ahead to the next song and start playing
            playNextSong(null);
        }
        else if (state == State.Paused) {
            // If we're paused, just continue playback and restore the 'foreground service' state.
            state = State.Playing;
            setUpAsForeground(songTitle + " (playing)");
            configAndStartMediaPlayer();
        }
    }

    void processPauseRequest() {
        if (state == State.Retrieving) {
            // If we are still retrieving media, clear the flag that indicates we should start
            // playing when we're ready
            mStartPlayingAfterRetrieve = false;
            return;
        }

        if (state == State.Playing) {
            // Pause media player and cancel the 'foreground service' state.
            state = State.Paused;
            mp.pause();
            relaxResources(false); // while paused, we always retain the MediaPlayer
            // do not give up audio focus
        }
    }

    void processRewindRequest() {
        if (state == State.Playing || state == State.Paused)
            mp.seekTo(0);
    }

    void processSkipRequest() {
        if (state == State.Playing || state == State.Paused) {
            tryToGetAudioFocus();
            playNextSong(null);
        }
    }

    void processStopRequest() {
        processStopRequest(false);
    }

    void processStopRequest(boolean force) {
        if (state == State.Playing || state == State.Paused || force) {
            state = State.Stopped;

            // let go of all resources...
            relaxResources(true);
            giveUpAudioFocus();

            // service is no longer necessary. Will be started again if needed.
            stopSelf();
        }
    }

    /**
     * Releases resources used by the service for playback. This includes the "foreground service"
     * status and notification, the wake locks and possibly the MediaPlayer.
     *
     * @param releaseMediaPlayer Indicates whether the Media Player should also be released or not
     */
    void relaxResources(boolean releaseMediaPlayer) {
        // stop being a foreground service
        stopForeground(true);

        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer && mp != null) {
            mp.reset();
            mp.release();
            mp = null;
        }

        // we can also release the Wifi lock, if we're holding it
        if (wifiLock.isHeld()) wifiLock.release();
    }

	/**
     * Отдаем аудио фокус системе
     */
    void giveUpAudioFocus() {
        if (audioFocus == AudioFocus.Focused && mAudioFocusHelper != null
                                && mAudioFocusHelper.abandonFocus())
            audioFocus = AudioFocus.NoFocusNoDuck;
    }

    /**
     * Reconfigures MediaPlayer according to audio focus settings and starts/restarts it. This
     * method starts/restarts the MediaPlayer respecting the current audio focus state. So if
     * we have focus, it will play normally; if we don't have focus, it will either leave the
     * MediaPlayer paused or set it to a low volume, depending on what is allowed by the
     * current focus settings. This method assumes mp != null, so if you are calling it,
     * you have to do so from a context where you are sure this is the case.
     */
    void configAndStartMediaPlayer() {
        if (audioFocus == AudioFocus.NoFocusNoDuck) {
            // If we don't have audio focus and can't duck, we have to pause, even if state
            // is State.Playing. But we stay in the Playing state so that we know we have to resume
            // playback once we get the focus back.
            if (mp.isPlaying()) mp.pause();
            return;
        }
        else if (audioFocus == AudioFocus.NoFocusCanDuck)
            mp.setVolume(DUCK_VOLUME, DUCK_VOLUME);  // we'll be relatively quiet
        else
            mp.setVolume(1.0f, 1.0f); // we can be loud

        if (!mp.isPlaying()) mp.start();
    }

    void processAddRequest(Intent intent) {
        // user wants to play a song directly by URL or path. The URL or path comes in the "data"
        // part of the Intent. This Intent is sent by {@link MainActivity} after the user
        // specifies the URL/path via an alert box.
        if (state == State.Retrieving) {
            // we'll play the requested URL right after we finish retrieving
            mWhatToPlayAfterRetrieve = intent.getData();
            mStartPlayingAfterRetrieve = true;
        }
        else if (state == State.Playing || state == State.Paused || state == State.Stopped) {
            Log.i(LOG, "Playing from URL/path: " + intent.getData().toString());
            tryToGetAudioFocus();
            playNextSong(intent.getData().toString());
        }
    }

    void tryToGetAudioFocus() {
        if (audioFocus != AudioFocus.Focused && mAudioFocusHelper != null
                        && mAudioFocusHelper.requestFocus())
            audioFocus = AudioFocus.Focused;
    }

    /**
     * Starts playing the next song. If manualUrl is null, the next song will be randomly selected
     * from our Media Retriever (that is, it will be a random song in the user's device). If
     * manualUrl is non-null, then it specifies the URL or path to the song that will be played
     * next.
     */
    void playNextSong(String manualUrl) {
        state = State.Stopped;
        relaxResources(false); // release everything except MediaPlayer

        try {
            MusicRetriever.Item playingItem = null;
            if (manualUrl != null) {
                // set the source of the media player to a manual URL or path
                createMediaPlayerIfNeeded();
                mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mp.setDataSource(manualUrl);
                isStreaming = manualUrl.startsWith("http:") || manualUrl.startsWith("https:");

                playingItem = new MusicRetriever.Item(0, null, manualUrl, null, 0);
            }
            else {
                isStreaming = false; // playing a locally available song

                playingItem = mRetriever.getRandomItem();
                if (playingItem == null) {
                    Toast.makeText(
		                    this,
                            "No available music to play. Place some music on your external storage "
                            + "device (e.g. your SD card) and try again.",
                            Toast.LENGTH_LONG
                    ).show();
                    processStopRequest(true); // stop everything!
                    return;
                }

                // set the source of the media player a a content URI
                createMediaPlayerIfNeeded();
                mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mp.setDataSource(getApplicationContext(), playingItem.getURI());
            }

            songTitle = playingItem.getTitle();

            state = State.Preparing;
            setUpAsForeground(songTitle + " (loading)");

            // starts preparing the media player in the background. When it's done, it will call
            // our OnPreparedListener (that is, the onPrepared() method on this class, since we set
            // the listener to 'this').
            //
            // Until the media player is prepared, we *cannot* call start() on it!
            mp.prepareAsync();

            // If we are streaming from the internet, we want to hold a Wifi lock, which prevents
            // the Wifi radio from going to sleep while the song is playing. If, on the other hand,
            // we are *not* streaming, we want to release the lock if we were holding it before.
            if (isStreaming) wifiLock.acquire();
            else if (wifiLock.isHeld()) wifiLock.release();
        }
        catch (IOException ex) {
            Log.e("MusicService", "IOException playing next song: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /** Called when media player is done playing current song. */
    @Override
    public void onCompletion(MediaPlayer player) {
        // The media player finished playing the current song, so we go ahead and start the next.
        playNextSong(null);
    }

    /** Called when media player is done preparing. */
    @Override
    public void onPrepared(MediaPlayer player) {
        // The media player is done preparing. That means we can start playing!
        state = State.Playing;
        updateNotification(songTitle + " (playing)");
        configAndStartMediaPlayer();
    }

    /** Updates the notification. */
    void updateNotification(String text) {
        PendingIntent pi = PendingIntent.getActivity(
		        getApplicationContext(),
		        0,
                new Intent(getApplicationContext(), MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        mNotificationBuilder.setContentText(text).setContentIntent(pi);
        mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
    }

    /**
     * Configures service as a foreground service. A foreground service is a service that's doing
     * something the user is actively aware of (such as playing music), and must appear to the
     * user as a notification. That's why we create the notification here.
     */
    void setUpAsForeground(String text) {
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
                .setContentTitle("RandomMusicPlayer")
                .setContentText(text)
                .setContentIntent(pi)
                .setOngoing(true);

        startForeground(NOTIFICATION_ID, mNotificationBuilder.build());
    }

    /**
     * Called when there's an error playing media. When this happens, the media player goes to
     * the Error state. We warn the user about the error and reset the media player.
     */
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Toast.makeText(getApplicationContext(), "Media player error! Resetting.", Toast.LENGTH_SHORT).show();
        Log.e(LOG, "Error: what=" + String.valueOf(what) + ", extra=" + String.valueOf(extra));

        state = State.Stopped;
        relaxResources(true);
        giveUpAudioFocus();
        return true; // true indicates we handled the error
    }

    @Override
    public void onGainedAudioFocus() {
        Toast.makeText(getApplicationContext(), "gained audio focus.", Toast.LENGTH_SHORT).show();
        audioFocus = AudioFocus.Focused;

        // restart media player with new focus settings
        if (state == State.Playing)
            configAndStartMediaPlayer();
    }

    @Override
    public void onLostAudioFocus(boolean canDuck) {
        Toast.makeText(
		        getApplicationContext(),
		        "lost audio focus." + (canDuck ? "can duck" : "no duck"), Toast.LENGTH_SHORT
        ).show();
        audioFocus = canDuck ? AudioFocus.NoFocusCanDuck : AudioFocus.NoFocusNoDuck;

        // start/restart/pause media player with new focus settings
        if (mp != null && mp.isPlaying())
            configAndStartMediaPlayer();
    }

    @Override
    public void onMusicRetrieverPrepared() {
        // Done retrieving!
        state = State.Stopped;

        // If the flag indicates we should start playing after retrieving, let's do that now.
        if (mStartPlayingAfterRetrieve) {
            tryToGetAudioFocus();
            playNextSong(mWhatToPlayAfterRetrieve == null ? null : mWhatToPlayAfterRetrieve.toString());
        }
    }


    @Override
    public void onDestroy() {
        // Service is being killed, so make sure we release our resources
        state = State.Stopped;
        relaxResources(true);
        giveUpAudioFocus();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }
}
