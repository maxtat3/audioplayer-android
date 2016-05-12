package edu.sintez.audioplayer.activity;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.text.Html;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import edu.sintez.audioplayer.R;
import edu.sintez.audioplayer.retriever.MetaDataRetriever;
import edu.sintez.audioplayer.model.Track;
import edu.sintez.audioplayer.utils.FileChooser;

/**
 * Activity meta data tag information from user selected audio track
 */
public class FileInfoActivity extends Activity {

	private static final String LOG = FileInfoActivity.class.getName();

	/**
	 * When no data of meta tag information - displayed this symbol.
	 */
	private static final String NO_DATA_TXT = "----";

	/**
	 * Thumbnail image width from audio track
	 */
	private static final int THB_IMAGE_WIDTH = 160;

	/**
	 * Thumbnail image height from audio track
	 */
	private static final int THB_IMAGE_HEIGHT = 160;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_file_info);

		Track track = null;
		Bundle bundle = getIntent().getExtras();
		if (bundle != null) {
			track = bundle.getParcelable(FileChooser.SEL_FILE_KEY);
			MetaDataRetriever mdr = new MetaDataRetriever();
			mdr.setsMetaData(track);
		}

		ImageView thumbnail = (ImageView) findViewById(R.id.iv_thumbnail);
		TextView tvFullFileName = (TextView) findViewById(R.id.tv_full_file_name);
		TextView tvInfoTitle = (TextView) findViewById(R.id.tv_info_title);

		if (track != null) {
			Bitmap bitmap = getAlbumThumbnail(track.getURI().toString());
			if (bitmap != null) {
				thumbnail.setImageBitmap(bitmap);
				thumbnail.setAdjustViewBounds(true);
				thumbnail.setLayoutParams(new LinearLayout.LayoutParams(THB_IMAGE_WIDTH, THB_IMAGE_HEIGHT));
			}

			tvFullFileName.setText(Html.fromHtml("<b>Name: </b> " + track.getFileName()));

			tvInfoTitle.setText(Html.fromHtml("<b>Title: </b>" + checkMetaData(track.getTitle())));
		}


	}

	/**
	 * Checked is empty or not meta data text.
	 *
	 * @param meta meta data text
	 * @return if meta data is empty returned constant {@link #NO_DATA_TXT}
	 *          otherwise returned this no changed meta data text.
	 */
	private String checkMetaData(String meta) {
		if (meta == null || meta.equals("")) return NO_DATA_TXT;
		else return meta;
	}

	/**
	 * Getting image thumbnail embedded in audio track if available.
	 *
	 * @param pathToTrack absolute path to audio track .
	 * @return received image from audio track, otherwise null.
	 */
	public Bitmap getAlbumThumbnail(String pathToTrack) {
		android.media.MediaMetadataRetriever mmr = new MediaMetadataRetriever();
		mmr.setDataSource(pathToTrack);
		byte [] data = mmr.getEmbeddedPicture();
		if(data != null) return BitmapFactory.decodeByteArray(data, 0, data.length);
		return null;
	}
}