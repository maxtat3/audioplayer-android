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

	public Track() {
	}

	public Track(Uri uri, String artist, String title, String album, long duration) {
		this.uri = uri;
		this.artist = artist;
		this.title = title;
		this.album = album;
		this.duration = duration;
	}

	public Uri getURI() {
		return uri;
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

	public void setUri(Uri uri) {
		this.uri = uri;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setAlbum(String album) {
		this.album = album;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}
}
