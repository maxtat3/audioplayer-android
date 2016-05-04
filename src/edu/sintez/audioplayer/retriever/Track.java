package edu.sintez.audioplayer.retriever;

import android.net.Uri;

/**
 * Represents music track.
 */
public class Track {
	private Uri uri;
	private String artist;
	private String title;
	private String album;
	private long duration;

	public Track(Uri uri, String artist, String title, String album, long duration) {
		this.uri = uri;
		this.artist = artist;
		this.title = title;
		this.album = album;
		this.duration = duration;
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
		return uri;
	}
}
