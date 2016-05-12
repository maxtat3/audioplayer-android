package edu.sintez.audioplayer.utils;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Type of item element on file system.
 * File is a general definition of the file system element.
 * For example, the directory is also a file contains a few inodes.
 */
public enum FileType implements Parcelable{
	FILE("File"),
	DIR("Folder"),
	PARENT_DIR("Parent dir");


	private String desc;

	FileType(String desc) {
		this.desc = desc;
	}

	public String getDesc() {
		return desc;
	}


	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(final Parcel dest, final int flags) {
		dest.writeInt(ordinal());
	}

	public static final Creator<FileType> CREATOR = new Creator<FileType>() {
		@Override
		public FileType createFromParcel(final Parcel source) {
			return FileType.values()[source.readInt()];
		}

		@Override
		public FileType[] newArray(final int size) {
			return new FileType[size];
		}
	};
}
