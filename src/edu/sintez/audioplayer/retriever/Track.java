package edu.sintez.audioplayer.retriever;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.MediaStore;

/**
 * Represents music track.
 */
public class Track {
	private long id;
	private String artist;
	private String title;
	private String album;
	private long duration;

	public Track(long id, String artist, String title, String album, long duration) {
		this.id = id;
		this.artist = artist;
		this.title = title;
		this.album = album;
		this.duration = duration;
	}

	public long getId() {
		return id;
	}

	public String getArtist() {
		return artist;
	}

	public String getTitle() {
		return title;
	}

	public String getAlbum() {
		return album;
	}

	public long getDuration() {
		return duration;
	}

	public Uri getURI() {
		return ContentUris.withAppendedId(
			MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
	}
}
