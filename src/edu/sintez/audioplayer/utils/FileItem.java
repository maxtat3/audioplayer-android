package edu.sintez.audioplayer.utils;

public class FileItem implements Comparable<FileItem> {
	private String name;
	private String data;
	private String path;

	public FileItem(String name, String data, String path) {
		this.name = name;
		this.data = data;
		this.path = path;
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

	@Override
	public int compareTo(FileItem item) {
		if (name != null)
			return this.name.toLowerCase().compareTo(item.getName().toLowerCase());
		else
			throw new IllegalArgumentException();
	}
}
