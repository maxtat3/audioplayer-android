package edu.sintez.audioplayer.app.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
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
import edu.sintez.audioplayer.app.utils.PlayListComparator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Shows player main direction elements. This activity shows: buttons, playlist, txt information elements, progress bar.
 * Handling audio tracks by passing from Intents to {@link MusicService}.
 */
public class MainActivity extends Activity implements
	View.OnClickListener,
	AdapterView.OnItemClickListener,
	AdapterView.OnItemLongClickListener,
	PrepareMusicRetrieverTask.MusicRetrieverPreparedListener, SeekBar.OnSeekBarChangeListener {

	private static final String LOG = MainActivity.class.getName();

	/**
	 * Request code for file chooser.
	 */
	public static final int FILE_CHOOSER_RQ = 1;

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
	 * Maximum position in seek bar (steps in progress bar line).
	 * This is default value for {@link SeekBar} view.
	 * This max position value must be changed called {@link SeekBar#setMax(int)} method.
	 */
	private static final int SEEK_BAR_MAX_POS = 100;

	/**
	 * Key which allow saving and restoring data. When this activity create a new.
	 *
	 * @see {@link #onSaveInstanceState(Bundle)}
	 * @see {@link #onRestoreInstanceState(Bundle)}
	 */
	public static final String SAVED_BUNDLE_KEY = MainActivity.class.getName() + "." + "save";

	/**
	 * Saving playlist key (for example when rotate display process).
	 *
	 * @see #tracks
	 */
	public static final String SAVED_TRACKS_LIST_KEY = MainActivity.class.getName() + "." + "tracks";

	/**
	 * Saving user selected position in playlist (for example when rotate display process).
	 *
	 * @see #selTrackPos
	 */
	public static final String SAVED_SEL_TRACK_POS_KEY = MainActivity.class.getName() + "." + "selTrackPos";

	private Button btnPlay;
	private Button btnPause;
	private Button btnNextSong;
	private Button btnPrevSong;
	private Button btnStop;
	private Button btnOpenFileChooser;
	private Button btnGetAllMusFromDevice;
	private Button btnClearPlaylist;

	private TextView tvCurrentTrackTime;
	private TextView tvAllTrackTime;

	/**
	 * Determine progress track line.
	 */
	private SeekBar sBarProgress;

	/**
	 * Playlist audio tracks.
	 */
	private ListView lvPlaylist;

	/**
	 * Handles for scanning for media and providing titles and URIs as we need.
	 */
	private MusicRetriever musRetriever;

	/**
	 * Adapter for {@link #lvPlaylist}.
	 */
	private ArrayAdapter<Track> adapter;

	/**
	 * This a tracks playlist.
	 * Tracks storage for displaying in {@link #lvPlaylist}.
	 * Note. Type this variable set is {@link ArrayList} because this collection must be
	 * put to {@link Bundle} object to save and restore when this Activity recreate.
	 */
	private ArrayList<Track> tracks = new ArrayList<Track>();

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
		btnOpenFileChooser = (Button) findViewById(R.id.btn_open_file_chooser);
		btnGetAllMusFromDevice = (Button) findViewById(R.id.btn_get_all_music_from_device);
		btnClearPlaylist = (Button) findViewById(R.id.btn_clear_playlist);

		btnPlay.setOnClickListener(this);
		btnPause.setOnClickListener(this);
		btnNextSong.setOnClickListener(this);
		btnPrevSong.setOnClickListener(this);
		btnStop.setOnClickListener(this);
		btnOpenFileChooser.setOnClickListener(this);
		btnGetAllMusFromDevice.setOnClickListener(this);
		btnClearPlaylist.setOnClickListener(this);

		lvPlaylist = (ListView) findViewById(R.id.lv_playlist);
		if (savedInstanceState == null) {
			adapter = new PlaylistAdapter(this, R.layout.pattern_playlist_item, tracks);
		} else {
			Bundle bundle = savedInstanceState.getBundle(SAVED_BUNDLE_KEY);
			restoreData(bundle);
			adapter = new PlaylistAdapter(this, R.layout.pattern_playlist_item, tracks);
			lvPlaylist.setSelection(selTrackPos);
		}
		lvPlaylist.setAdapter(adapter);
		lvPlaylist.setOnItemClickListener(this);
		lvPlaylist.setOnItemLongClickListener(this);

		tvCurrentTrackTime = (TextView) findViewById(R.id.current_track_time);
		tvAllTrackTime = (TextView) findViewById(R.id.all_track_time);

		sBarProgress = (SeekBar) findViewById(R.id.sbar_track_progress);
		sBarProgress.setOnSeekBarChangeListener(this);

		registerReceivers();
		createSearchInActionBar();
		receivedAudioFromIntent();
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBundle(SAVED_BUNDLE_KEY, savingData());
	}

	/**
	 * Saving data when called {@link #onSaveInstanceState(Bundle)} method.
	 *
	 * @return Bundle with data for saving
	 */
	private Bundle savingData() {
		Bundle bundle = new Bundle();
		bundle.putParcelableArrayList(SAVED_TRACKS_LIST_KEY, tracks);
		bundle.putInt(SAVED_SEL_TRACK_POS_KEY, selTrackPos);
		return bundle;
	}

	/**
	 * Restore data when called {@link #onRestoreInstanceState(Bundle)} method.
	 *
	 * @param bundle with data for restoring
	 */
	private void restoreData(Bundle bundle) {
		if (bundle != null) {
			tracks = bundle.getParcelableArrayList(SAVED_TRACKS_LIST_KEY);
			selTrackPos = bundle.getInt(SAVED_SEL_TRACK_POS_KEY, 0);
		}
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

		} else if (view == btnOpenFileChooser) {
			startActivityForResult(new Intent(this, FileChooserActivity.class), FILE_CHOOSER_RQ);

		} else if (view == btnGetAllMusFromDevice) {
			musRetriever = new MusicRetriever(getContentResolver());
			(new PrepareMusicRetrieverTask(musRetriever, this)).execute();

		} else if (view == btnClearPlaylist) {
			if (!tracks.isEmpty()){
				tracks.clear();
				adapter.clear();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (tracks.isEmpty()) {
			Toast.makeText(this, "Playlist is empty, nothing to sort", Toast.LENGTH_SHORT).show();
			return false;
		}

		switch (item.getItemId()) {
			case R.id.menu_sort_by_title:
				Collections.sort(tracks, PlayListComparator.getCompByTitle());
				break;

			case R.id.menu_sort_by_artist:
				Collections.sort(tracks, PlayListComparator.getCompByArtist());
				break;

			case R.id.menu_sort_by_album:
				Collections.sort(tracks, PlayListComparator.getCompByAlbum());
				break;
			case R.id.menu_sort_by_duration:
				Collections.sort(tracks, PlayListComparator.getCompByDuration());
				break;
		}
		item.setChecked(true);
		adapter.notifyDataSetChanged();
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Start playing selected audio track. Selected audio track pointed
	 * {@link #selTrackPos} counter.
	 */
	private void playTrack() {
		if (tracks.isEmpty()){
			Toast.makeText(this, "Playlist is empty", Toast.LENGTH_SHORT).show();
			return;
		}

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
		else {
			--selTrackPos;
			showNoMoreTracksMsg();
		}
	}

	/**
	 * Start playing previous audio track.
	 */
	private void playPrevTrack() {
		if (--selTrackPos >= 0) playTrack();
		else {
			selTrackPos = 0;
			showNoMoreTracksMsg();
		}
	}

	/**
	 * When no available audio tracks in playlist, must be showed this massage.
	 */
	private void showNoMoreTracksMsg() {
		Toast.makeText(this, "There are no more audio tracks !", Toast.LENGTH_LONG).show();
	}

	/**
	 * Open audio file data from intent.
	 * When user choose this app to open music file, this app from intent received
	 * audio file uri, extracted file and meta information, add to playlist and start playing.
	 */
	private void receivedAudioFromIntent() {
		if (getIntent().getAction() == null) return;

		String action = getIntent().getAction();
		if (action.equals(Intent.ACTION_MAIN)) return;

		if (action.equals(Intent.ACTION_VIEW)) {
			Uri data = getIntent().getData();
			if (data != null) {
				String absPath = FileInfoActivity.getAbsPathFromURI(this, data);
				MetaDataRetriever mdr = new MetaDataRetriever();
				File f = new File(absPath);
				Track track = new Track();
				track.setUri(Uri.parse(absPath));
				track.setFileName(f.getName());
				track.setFileSize(FileChooserActivity.roundDouble(f.length()/1024/1024));
				mdr.setsMetaData(track);

				tracks.add(track);
				selTrackPos = tracks.size() - 1;
				playTrack();
			}
		}
	}

	/**
	 * Register receivers for this app from intent filters.
	 */
	private void registerReceivers() {
		LocalBroadcastManager.getInstance(this)
			.registerReceiver(trackTimeReceiver, new IntentFilter(MusicService.TRACK_TIME_KEY));
		LocalBroadcastManager.getInstance(this)
			.registerReceiver(trackTimeReceiver, new IntentFilter(MusicService.GET_NEXT_TRACK_KEY));
	}

	/**
	 * For search audio tracks (fom title now) creating search view in Actionbar.
	 */
	private void createSearchInActionBar() {
		ActionBar actionBar;
		EditText etSearch;
		if (getActionBar() != null) {
			actionBar = getActionBar();
			actionBar.setCustomView(R.layout.pattern_actionbar_search);
			etSearch = (EditText) actionBar.getCustomView().findViewById(R.id.et_ab);

			etSearch.addTextChangedListener(new SearchListener());
			actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		}
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
	public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
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
		playTrack();
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

	@Override
	public void onMusicRetrieverPrepared() {
		adapter.addAll(musRetriever.getAllAudioTracks());
		adapter.notifyDataSetChanged();
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
			} else if (i.getAction().equals(MusicService.GET_NEXT_TRACK_KEY)) {
				playNextTrack();
			}
		}
	};

	/**
	 * Filtration at {@link Track#title} field when entered symbols in search edit text.
	 */
	private class SearchListener implements TextWatcher {
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			if (!s.toString().equals("")) adapter.getFilter().filter(s);
		}

		@Override
		public void afterTextChanged(Editable s) {
		}
	}
}