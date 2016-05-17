package edu.sintez.audioplayer.app.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Supporting audio formats.
 * Represents extension this file audio formats.
 */
public enum SupportedAudioFormat implements Parcelable{

	MP3("mp3"),
	FLAC("flac"),
	NOT_DEFINED("");

	private String ext;

	SupportedAudioFormat(String ext) {
		this.ext = ext;
	}

	public String getExt() {
		return ext;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(final Parcel dest, final int flags) {
		dest.writeInt(ordinal());
	}

	public static final Creator<SupportedAudioFormat> CREATOR = new Creator<SupportedAudioFormat>() {
		@Override
		public SupportedAudioFormat createFromParcel(final Parcel source) {
			return SupportedAudioFormat.values()[source.readInt()];
		}

		@Override
		public SupportedAudioFormat[] newArray(final int size) {
			return new SupportedAudioFormat[size];
		}
	};
}
