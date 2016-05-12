package edu.sintez.audioplayer.retriever;

import android.media.MediaMetadataRetriever;
import edu.sintez.audioplayer.model.Track;

/**
 * Handles for eject meta data information from audio tracks.
 */
public class MetaDataRetriever {

	private static final String LOG = MetaDataRetriever.class.getName();
	private MediaMetadataRetriever metaRetriever;

	public MetaDataRetriever() {
		metaRetriever = new MediaMetadataRetriever();
	}

	/**
	 * Sets the meta data values of the {@link Track} object is passed by reference !
	 * Track must be contained URI from set data source in {@link MediaMetadataRetriever}
	 *
	 * @param track audio track in which sets meta data values
	 * @see Track
	 */
	public void setsMetaData(Track track) {
		if (track.getURI() == null) throw new IllegalArgumentException("Track must be contained URI !");

		metaRetriever.setDataSource(track.getURI().toString());
		track.setArtist(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
		track.setTitle(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
		track.setAlbum(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM));
		track.setDuration(Long.parseLong(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)));

	}

}
