package edu.sintez.audioplayer.retriever;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents music track.
 */
public class Track implements Parcelable{
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

	public Track(Parcel p) {
		uri = Uri.parse(p.readString());
		artist = p.readString();
		title = p.readString();
		album = p.readString();
		duration = p.readLong();
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


	public static final Parcelable.Creator<Track> CREATOR = new Parcelable.Creator<Track>() {
		@Override
		public Track createFromParcel(Parcel source) {
			return new Track(source);
		}

		@Override
		public Track[] newArray(int size) {
			return new Track[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel p, int flags) {
		p.writeString(uri.toString());
		p.writeString(artist);
		p.writeString(title);
		p.writeString(album);
		p.writeLong(duration);
	}
}
