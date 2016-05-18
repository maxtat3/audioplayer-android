package edu.sintez.audioplayer.app.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents music track.
 */
public class Track implements Parcelable{
	private Uri uri;
	private String fileName;
	private double fileSize; //track (file) size in MegaBytes
	private String artist;
	private String title;
	private String album;
	private long duration;
	private int bitrate; //audio track bitrate in kbps

	public Track() {
	}

	public Track(Uri uri, String fileName, double fileSize, String artist, String title, String album,
		long duration, int bitrate) {
		this.uri = uri;
		this.fileName = fileName;
		this.fileSize = fileSize;
		this.artist = artist;
		this.title = title;
		this.album = album;
		this.duration = duration;
		this.bitrate = bitrate;
	}

	public Track(Parcel p) {
		uri = Uri.parse(p.readString());
		fileName = p.readString();
		fileSize = p.readDouble();
		artist = p.readString();
		title = p.readString();
		album = p.readString();
		duration = p.readLong();
		bitrate = p.readInt();
	}

	public Uri getURI() {
		return uri;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public double getFileSize() {
		return fileSize;
	}

	public void setFileSize(double fileSize) {
		this.fileSize = fileSize;
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

	public int getBitrate() {
		return bitrate;
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

	public void setBitrate(int bitrate) {
		this.bitrate = bitrate;
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
		p.writeString(fileName);
		p.writeDouble(fileSize);
		p.writeString(artist);
		p.writeString(title);
		p.writeString(album);
		p.writeLong(duration);
		p.writeInt(bitrate);
	}

	@Override
	public String toString() {
		return title;
	}
}
