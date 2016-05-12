package edu.sintez.audioplayer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import edu.sintez.audioplayer.R;
import edu.sintez.audioplayer.model.Track;

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
			vh.artist = (TextView) convertView.findViewById(R.id.tv_artist);
			vh.title = (TextView) convertView.findViewById(R.id.tv_title);
			vh.album = (TextView) convertView.findViewById(R.id.tv_album);
			vh.duration = (TextView) convertView.findViewById(R.id.tv_duration);

			convertView.setTag(vh);
		}

		ViewHolder vh = (ViewHolder) convertView.getTag();
		Track track = getItem(pos);
		vh.artist.setText(track.getArtist());
		vh.title.setText(track.getTitle());
		vh.album.setText(track.getAlbum());
		vh.duration.setText(String.valueOf(track.getDuration()));

		return convertView;
	}

	public static class ViewHolder {
		public TextView artist;
		public TextView title;
		public TextView album;
		public TextView duration;
	}

}
