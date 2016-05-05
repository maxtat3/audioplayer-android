package edu.sintez.audioplayer.utils;

/**
 * Supporting audio formats.
 * Represents extension this file audio formats.
 */
public enum SupportedAudioFormat {

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
}
