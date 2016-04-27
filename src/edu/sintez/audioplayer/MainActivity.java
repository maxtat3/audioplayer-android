package edu.sintez.audioplayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.*;
import edu.sintez.audioplayer.retriever.MusicRetriever;
import edu.sintez.audioplayer.retriever.PrepareMusicRetrieverTask;
import edu.sintez.audioplayer.service.MusicService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Main activity: shows media player buttons. This activity shows the media player buttons and
 * lets the user click them. No media handling is done here -- everything is done by passing
 * Intents to our {@link MusicService}.
 * */
public class MainActivity extends Activity implements View.OnClickListener,
	AdapterView.OnItemClickListener, PrepareMusicRetrieverTask.MusicRetrieverPreparedListener {

	private static final String LOG = MainActivity.class.getName();

	/**
	 * The URL we suggest as default when adding by URL. This is just so that the user doesn't
	 * have to find an URL to test this sample.
	 */
	private static final String SUGGESTED_URL = "http://www.vorbis.com/music/Epoq-Lepidoptera.ogg";

	/**
	 * Handles for scanning for media and providing titles and URIs as we need.
	 */
	private MusicRetriever retriever;

	private Button btnPlay;
	private Button btnPause;
	private Button btnNextSong;
	private Button btnPrevSong;
	private Button btnStop;
	private Button btnOpenFromURL;
	private Button btnOpenPlaylist;
	private Button btnGetAllMusFromDevice;
	private ListView lvPlaylist;
	ArrayAdapter<String> adapter;
	private List<String> data = new ArrayList<String>(Arrays.asList("item 1", "item 2", "item 3", "item 4", "/mnt/sdcard/Download/music.mp3"));

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
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, data);
		lvPlaylist.setAdapter(adapter);
		lvPlaylist.setOnItemClickListener(this);
	}

	@Override
	public void onClick(View view) {
		if (view == btnPlay)
			startService(new Intent(MusicService.ACTION_PLAY));
		else if (view == btnPause)
			startService(new Intent(MusicService.ACTION_PAUSE));
		else if (view == btnNextSong)
			startService(new Intent(MusicService.ACTION_NEXT));
		else if (view == btnPrevSong)
			startService(new Intent(MusicService.ACTION_PREV));
		else if (view == btnStop)
			startService(new Intent(MusicService.ACTION_STOP));
		else if (view == btnOpenFromURL)
			showUrlDialog();
		else if (view == btnOpenPlaylist)
			Log.d(LOG, "Do open playlist.");
		else if (view == btnGetAllMusFromDevice) {
			retriever = new MusicRetriever(getContentResolver());
			(new PrepareMusicRetrieverTask(retriever, this)).execute();

		}
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
		Log.d(LOG, "position = " + position);
	}

	@Override
	public void onMusicRetrieverPrepared() {
		adapter.addAll(retriever.getAllAudioTracks().get(0).getTitle());
		adapter.notifyDataSetChanged();
	}
}