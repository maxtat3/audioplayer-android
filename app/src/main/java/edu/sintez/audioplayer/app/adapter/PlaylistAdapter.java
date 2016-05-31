package edu.sintez.audioplayer.app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import edu.sintez.audioplayer.R;
import edu.sintez.audioplayer.app.model.Track;
import edu.sintez.audioplayer.app.utils.Utilities;

import java.util.List;

/**
 * Fill playlist items information from audio tracks .
 */
public class PlaylistAdapter extends ArrayAdapter<Track> {

	private static final String LOG = PlaylistAdapter.class.getName();


	public PlaylistAdapter(Context context, int resource, List<Track> objects) {
		super(context, resource, objects);
	}


	@Override
	public View getView(int pos, View convertView, ViewGroup parent) {
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.pattern_playlist_item, parent, false);

			ViewHolder vh = new ViewHolder();
			vh.imgIcon = (ImageView) convertView.findViewById(R.id.iv_play_item_image);
			vh.title = (TextView) convertView.findViewById(R.id.tv_title);
			vh.artist = (TextView) convertView.findViewById(R.id.tv_artist);
			vh.album = (TextView) convertView.findViewById(R.id.tv_album);
			vh.duration = (TextView) convertView.findViewById(R.id.tv_duration);
			vh.fileSize = (TextView) convertView.findViewById(R.id.tv_file_size);
			vh.audioFormat = (TextView) convertView.findViewById(R.id.tv_audio_format);

			convertView.setTag(vh);
		}

		ViewHolder vh = (ViewHolder) convertView.getTag();
		Track track = getItem(pos);
//		vh.imgIcon.set
		vh.title.setText(track.getTitle());
		vh.artist.setText(track.getArtist());
		vh.album.setText(track.getAlbum());
		vh.duration.setText( String.valueOf(Utilities.getTimeText(track.getDuration())) );
		vh.fileSize.setText(String.valueOf(track.getFileSize()) + " MB");
		vh.audioFormat.setText(getFileExt(track.getFileName()));

		return convertView;
	}

	public static class ViewHolder {
		public ImageView imgIcon;
		public TextView title;
		public TextView artist;
		public TextView album;
		public TextView duration;
		public TextView fileSize;
		public TextView audioFormat;
	}

	/**
	 * Extracted file extension from full file name within extension.
	 * In this method simple algorithm and he well work for correct file naming.
	 * But if file name for example audio.track.mp3 this method
	 * returned "not def" constant.
	 *
	 * @param fullFileName file name within extension
	 * @return file extension
	 */
	public String getFileExt(String fullFileName) {
		String ext = "Not def";
		int index = fullFileName.lastIndexOf(".");
		if (index > 0) ext = fullFileName.substring(index + 1);
		return ext;
	}

}
