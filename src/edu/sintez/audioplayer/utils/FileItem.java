package edu.sintez.audioplayer.utils;

public class FileItem implements Comparable<FileItem> {
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
}
