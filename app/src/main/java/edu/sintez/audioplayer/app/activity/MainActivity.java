package edu.sintez.audioplayer.app.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.*;
import edu.sintez.audioplayer.R;
import edu.sintez.audioplayer.app.adapter.PlaylistAdapter;
import edu.sintez.audioplayer.app.retriever.MetaDataRetriever;
import edu.sintez.audioplayer.app.retriever.MusicRetriever;
import edu.sintez.audioplayer.app.retriever.PrepareMusicRetrieverTask;
import edu.sintez.audioplayer.app.model.Track;
import edu.sintez.audioplayer.app.service.MusicService;
import edu.sintez.audioplayer.app.model.FileItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Main activity: shows media player buttons. This activity shows the media player buttons and
 * lets the user click them. No media handling is done here -- everything is done by passing
 * Intents to our {@link MusicService}.
 */
public class MainActivity extends Activity implements
	View.OnClickListener,
	AdapterView.OnItemClickListener,
	AdapterView.OnItemLongClickListener,
	PrepareMusicRetrieverTask.MusicRetrieverPreparedListener, SeekBar.OnSeekBarChangeListener {

	private static final String LOG = MainActivity.class.getName();

	/**
	 * Request code for file chooser
	 */
	public static final int FILE_CHOOSER_RQ = 1;

	/**
	 * The URL we suggest as default when adding by URL. This is just so that the user doesn't
	 * have to find an URL to test this sample.
	 */
	private static final String SUGGESTED_URL = "http://www.vorbis.com/music/Epoq-Lepidoptera.ogg";

	/**
	 * Key which FileInfoActivity receive audio track. In this audio track
	 * all fields filled. So track contained full file and meta information.
	 *
	 * @see FileInfoActivity
	 * @see Track
	 */
	public static final String PLAYLIST_INFO_FILE_KEY = MainActivity.class.getName() + "." + "file_info";

	/**
	 * Key which MusicService receive audio track and do playing them.
	 *
	 * @see MusicService
	 * @see Track
	 */
	public static final String SERVICE_PLAYING_TRACK_KEY = MainActivity.class.getName() + "." + "service_playing_track";

	/**
	 * Key which MusicService receive new position in ms time to jump inside during playing audio track.
	 */
	public static final String SERVICE_JUMP_TO_POS_KEY = MainActivity.class.getName() + "." + "service_jump_to_pos";

	/**
	 * Maximum position in seek bar (progress bar line).
	 * This is default value for {@link SeekBar} view.
	 * This max position value must be changed called {@link SeekBar#setMax(int)} method.
	 */
	private static final int SEEK_BAR_MAX_POS = 100;

	private Button btnPlay;
	private Button btnPause;
	private Button btnNextSong;
	private Button btnPrevSong;
	private Button btnStop;
	private Button btnOpenFromURL;
	private Button btnOpenPlaylist;
	private Button btnGetAllMusFromDevice;

	private TextView tvCurrentTrackTime;
	private TextView tvAllTrackTime;

	/**
	 * Determine progress track line.
	 */
	private SeekBar sBarProgress;

	/**
	 * Playlist audio tracks
	 */
	private ListView lvPlaylist;

	/**
	 * Handles for scanning for media and providing titles and URIs as we need.
	 */
	private MusicRetriever musRetriever;

	/**
	 * Adapter for {@link #lvPlaylist} .
	 */
	private ArrayAdapter<Track> adapter;

	/**
	 * Tracks storage displaying in playlist
	 */
	private List<Track> tracks = new ArrayList<Track>();

	/**
	 * Selected audio track position when user clicked in playlist item.
	 * Default selected position is 0.
	 */
	private int selTrackPos = 0;

	/**
	 * Total playing time current audio track in ms.
	 * This time is set when audio track start playing.
	 */
	private int totalTrackTime = 0;

	/**
	 * Dynamic updatable playing audio track time.
	 * This time is set when audio track is playing and update every 1 sec.
	 */
	private int currentTrackTime = 0;

	/**
	 * Position of {@link SeekBar} in units for jumping in side audio track.
	 * Value is written here when user drag {@link SeekBar} progress bar line.
	 * This new value allow jumped to random playing audio track position.
	 */
	private int sBarProgressPos;

	/**
	 * Called when the activity is first created. Here, we simply set the event listeners and
	 * start the background service ({@link MusicService}) that will handle the actual media
	 * playback.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		btnPlay = (Button) findViewById(R.id.btn_play);
		btnPause = (Button) findViewById(R.id.btn_pause);
		btnNextSong = (Button) findViewById(R.id.btn_next_song);
		btnPrevSong = (Button) findViewById(R.id.btn_prev_song);
		btnStop = (Button) findViewById(R.id.btn_stop);
		btnOpenFromURL = (Button) findViewById(R.id.btn_open_from_url);
		btnOpenPlaylist = (Button) findViewById(R.id.btn_open_playlist);
		btnGetAllMusFromDevice = (Button) findViewById(R.id.btn_get_all_music_from_device);

		btnPlay.setOnClickListener(this);
		btnPause.setOnClickListener(this);
		btnNextSong.setOnClickListener(this);
		btnPrevSong.setOnClickListener(this);
		btnStop.setOnClickListener(this);
		btnOpenFromURL.setOnClickListener(this);
		btnOpenPlaylist.setOnClickListener(this);
		btnGetAllMusFromDevice.setOnClickListener(this);

		lvPlaylist = (ListView) findViewById(R.id.lv_playlist);
		adapter = new PlaylistAdapter(this, R.layout.pattern_playlist_item, tracks);
		lvPlaylist.setAdapter(adapter);
		lvPlaylist.setOnItemClickListener(this);
		lvPlaylist.setOnItemLongClickListener(this);

		tvCurrentTrackTime = (TextView) findViewById(R.id.current_track_time);
		tvAllTrackTime = (TextView) findViewById(R.id.all_track_time);

		LocalBroadcastManager.getInstance(this).registerReceiver(
			trackTimeReceiver, new IntentFilter(MusicService.TRACK_TIME_KEY));

		sBarProgress = (SeekBar) findViewById(R.id.sbar_track_progress);
		sBarProgress.setOnSeekBarChangeListener(this);
	}

	@Override
	public void onClick(View view) {
		if (view == btnPlay) {
			playTrack();

		} else if (view == btnPause)
			startService(new Intent(MusicService.ACTION_PAUSE));

		else if (view == btnNextSong) {
			playNextTrack();

		} else if (view == btnPrevSong) {
			playPrevTrack();

		} else if (view == btnStop) {
			startService(new Intent(MusicService.ACTION_STOP));
			sBarProgress.setProgress(0);

		} else if (view == btnOpenFromURL)
			showUrlDialog();
		else if (view == btnOpenPlaylist) {
			startActivityForResult(new Intent(this, FileChooserActivity.class), FILE_CHOOSER_RQ);
		} else if (view == btnGetAllMusFromDevice) {
			musRetriever = new MusicRetriever(getContentResolver());
			(new PrepareMusicRetrieverTask(musRetriever, this)).execute();
		}
	}

	/**
	 * Start playing selected audio track. Selected audio track pointed
	 * {@link #selTrackPos} counter.
	 */
	private void playTrack() {
		Bundle bundle = new Bundle();
		bundle.putParcelable(SERVICE_PLAYING_TRACK_KEY, tracks.get(selTrackPos));
		Intent i = new Intent(MusicService.ACTION_PLAY);
		i.putExtras(bundle);
		startService(i);
	}

	/**
	 * Start playing next audio track.
	 */
	private void playNextTrack() {
		if (tracks.size() > ++selTrackPos) playTrack();
		else --selTrackPos;
	}

	/**
	 * Start playing previous audio track.
	 */
	private void playPrevTrack() {
		if (--selTrackPos >= 0) playTrack();
		else selTrackPos = 0;
	}

	/**
	 * Shows an alert dialog where the user can input a URL. After showing the dialog, if the user
	 * confirms, sends the appropriate intent to the {@link MusicService} to cause that URL to be
	 * played.
	 */
	void showUrlDialog() {
		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
		alertBuilder.setTitle("Manual Input");
		alertBuilder.setMessage("Enter a URL (must be http://)");
		final EditText input = new EditText(this);
		alertBuilder.setView(input);

		input.setText(SUGGESTED_URL);

		alertBuilder.setPositiveButton("Play!", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dlg, int whichButton) {
				// Send an intent with the URL of the song to play. This is expected by
				// MusicService.
				Intent i = new Intent(MusicService.ACTION_URL);
				Uri uri = Uri.parse(input.getText().toString());
				i.setData(uri);
				startService(i);
			}
		});
		alertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dlg, int whichButton) {}
		});

		alertBuilder.show();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
			case KeyEvent.KEYCODE_HEADSETHOOK:
				startService(new Intent(MusicService.ACTION_TOGGLE_PLAYBACK));
				return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		selTrackPos = position;
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		Bundle bundle = new Bundle();
		bundle.putParcelable(PLAYLIST_INFO_FILE_KEY, tracks.get(position));
		Intent infoIntent = new Intent(this, FileInfoActivity.class);
		infoIntent.putExtras(bundle);
		startActivity(infoIntent);
		return true;
	}

	// seek bar change listeners callback methods
	// in this methods we is handle progress jumping in inside playing track
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		sBarProgressPos = progress;
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		Intent i = new Intent(MusicService.ACTION_JUMP_TO);
		i.putExtra(SERVICE_JUMP_TO_POS_KEY, convertSeekBarPosToMs(sBarProgressPos));
		startService(i);
	}

	/**
	 * Update displaying current and total track time in play process.
	 */
	private void updateDisplayTime() {
		tvCurrentTrackTime.setText(FileInfoActivity.getTimeText(currentTrackTime));
		tvAllTrackTime.setText(FileInfoActivity.getTimeText(totalTrackTime));
	}

	/**
	 * Update progress bar in play process.
	 */
	private void updateSeekBar() {
		sBarProgress.setProgress(convertMsToSeekBarPos(currentTrackTime));
	}

	/**
	 * Converting {@link SeekBar} position to ms time when user change position in progress line.
	 *
	 * @param pos new progress position
	 * @return progress represents as ms
	 * @see SeekBar
	 * @see #totalTrackTime
	 */
	private int convertSeekBarPosToMs(int pos) {
		return (pos * totalTrackTime) / SEEK_BAR_MAX_POS;
	}

	/**
	 * Converting ms time to position for {@link SeekBar}.
	 * This method must be called for automatic update {@link SeekBar} in playing process.
	 *
	 * @param timeMs time in ms
	 * @return progress represents as positions unit for {@link SeekBar}
	 * @see SeekBar
	 * @see #totalTrackTime
	 */
	private int convertMsToSeekBarPos(int timeMs) {
		return (timeMs * SEEK_BAR_MAX_POS) / totalTrackTime;
	}

	@Override
	public void onMusicRetrieverPrepared() {
		adapter.addAll(musRetriever.getAllAudioTracks());
		// after add items to adapter in tracks collection size equal to this added item elements
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (intent == null) return;

		if (requestCode == FILE_CHOOSER_RQ) {
			ArrayList<FileItem> selFilesPaths = intent.getParcelableArrayListExtra(FileChooserActivity.SELECTED_FILES_LIST_KEY);
			MetaDataRetriever mdr = new MetaDataRetriever();
			for (FileItem selFile : selFilesPaths) {
				Track track = new Track();
				track.setUri(Uri.parse(selFile.getPath()));
				track.setFileName(selFile.getName());
				track.setFileSize(selFile.getSize());
				mdr.setsMetaData(track);
				adapter.add(track);
			}
		}
	}

	/**
	 * Though this receiver {@link MusicService} update current playing time
	 * and total playing time track.
	 */
	private BroadcastReceiver trackTimeReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent i) {
			if (i.getAction().equals(MusicService.TRACK_TIME_KEY)) {
				currentTrackTime = i.getIntExtra(MusicService.PLAY_TIME_CURRENT, 0);
				totalTrackTime = i.getIntExtra(MusicService.PLAY_TIME_ALL, 0);

				updateDisplayTime();
				updateSeekBar();
			}
		}
	};
}