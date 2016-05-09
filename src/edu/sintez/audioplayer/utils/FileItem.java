package edu.sintez.audioplayer.utils;

import android.os.Parcel;
import android.os.Parcelable;

public class FileItem implements Comparable<FileItem>, Parcelable {
	private String name;
	private String data;
	private String path;
	private SupportedAudioFormat format = SupportedAudioFormat.NOT_DEFINED;


	public FileItem(String name, String data, String path) {
		this.name = name;
		this.data = data;
		this.path = path;
	}

	public FileItem(String name, String data, String path, SupportedAudioFormat format) {
		this.name = name;
		this.data = data;
		this.path = path;
		this.format = format;
	}

	public FileItem(Parcel p) {
		name = p.readString();
		data = p.readString();
		path = p.readString();
		format = p.readParcelable(SupportedAudioFormat.class.getClassLoader());
	}

	public String getName() {
		return name;
	}

	public String getData() {
		return data;
	}

	public String getPath() {
		return path;
	}

	public SupportedAudioFormat getFormat() {
		return format;
	}

	@Override
	public int compareTo(FileItem item) {
		if (name != null)
			return this.name.toLowerCase().compareTo(item.getName().toLowerCase());
		else
			throw new IllegalArgumentException();
	}

	public static final Parcelable.Creator<FileItem> CREATOR = new Parcelable.Creator<FileItem>() {
		@Override
		public FileItem createFromParcel(Parcel source) {
			return new FileItem(source);
		}

		@Override
		public FileItem[] newArray(int size) {
			return new FileItem[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel p, int flags) {
		p.writeString(name);
		p.writeString(data);
		p.writeString(path);
		p.writeParcelable(format, PARCELABLE_WRITE_RETURN_VALUE);
	}
}
