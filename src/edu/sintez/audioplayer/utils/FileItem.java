package edu.sintez.audioplayer.utils;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Element on file system.
 */
public class FileItem implements Comparable<FileItem>, Parcelable {
	/**
	 * Type of this item element on file system. Values than can
	 * takes this variable are {@link FileType#FILE}, {@link FileType#DIR},
	 * {@link FileType#PARENT_DIR} .
	 */
	private FileType type;

	/**
	 * File or Dir name.
	 */
	private String name;

	/**
	 * Absolute path to file or dir.
	 */
	private String path;

	/**
	 * Size of file in MegaBytes. Used for files only.
	 */
	private double size = 0.0;

	/**
	 * Determinate file format. Used for files only.
	 * In context this applications represents audio files only.
	 */
	private SupportedAudioFormat format = SupportedAudioFormat.NOT_DEFINED;


	/**
	 * Constructed directory item element.
	 *
	 * @param type type of this directory on file system.
	 *             Values than can takes for create directory
	 *             are {@link FileType#DIR}, {@link FileType#PARENT_DIR}.
	 * @param name directory name
	 * @param path absolute path to dir.
	 */
	public FileItem(FileType type, String name, String path) {
		this.type = type;
		this.name = name;
		this.path = path;
	}

	/**
	 * Constructed file item element.
	 *
	 * @param type type of this file on file system.
	 *             Values than can takes for create file is {@link FileType#FILE}.
	 * @param name file name
	 * @param path absolute path to file.
	 * @param size size in MegaBytes.
	 * @param format file audio format. Only supporting player audio formats displayed in file chooser.
	 */
	public FileItem(FileType type, String name, String path, double size, SupportedAudioFormat format) {
		this.type = type;
		this.name = name;
		this.path = path;
		this.size = size;
		this.format = format;
	}

	public FileItem(Parcel p) {
		type = p.readParcelable(FileType.class.getClassLoader());
		name = p.readString();
		path = p.readString();
		size = p.readDouble();
		format = p.readParcelable(SupportedAudioFormat.class.getClassLoader());
	}


	public FileType getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public String getPath() {
		return path;
	}

	public double getSize() {
		return size;
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
		p.writeParcelable(type, PARCELABLE_WRITE_RETURN_VALUE);
		p.writeString(name);
		p.writeString(path);
		p.writeDouble(size);
		p.writeParcelable(format, PARCELABLE_WRITE_RETURN_VALUE);
	}
}
