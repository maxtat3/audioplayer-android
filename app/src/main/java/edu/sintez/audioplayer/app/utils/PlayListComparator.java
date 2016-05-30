package edu.sintez.audioplayer.app.utils;

import edu.sintez.audioplayer.app.model.Track;

import java.util.Comparator;

/**
 * Available comparators by for filtering (sorting) playlist items.
 * This comparators are applied for {@link Track} class.
 */
public class PlayListComparator {

	private static ComparatorByTitle compByTitle;
	private static ComparatorByArtist compByArtist; //this compare by authors
	private static ComparatorByAlbum compByAlbum;
	private static ComparatorByDuration compByDuration;


	/**
	 * Comparator by {@link Track#title} field.
	 *
	 * @return comparator object by title
	 */
	public static Comparator<Track> getCompByTitle() {
		if (compByTitle == null){
			compByTitle = new ComparatorByTitle();
		}
		return compByTitle;
	}

	/**
	 * Comparator by {@link Track#artist} field.
	 *
	 * @return comparator object by artist
	 */
	public static Comparator<Track> getCompByArtist() {
		if (compByArtist == null){
			compByArtist = new ComparatorByArtist();
		}
		return compByArtist;
	}

	/**
	 * Comparator by {@link Track#album} field.
	 *
	 * @return comparator object by album
	 */
	public static Comparator<Track> getCompByAlbum() {
		if (compByAlbum == null){
			compByAlbum = new ComparatorByAlbum();
		}
		return compByAlbum;
	}

	/**
	 * Comparator by {@link Track#duration} field.
	 *
	 * @return comparator object by duration
	 */
	public static Comparator<Track> getCompByDuration() {
		if (compByDuration == null){
			compByDuration = new ComparatorByDuration();
		}
		return compByDuration;
	}


	private static class ComparatorByTitle implements Comparator<Track>{
		@Override
		public int compare(Track lhs, Track rhs) {
			if (lhs.getTitle() == null || rhs.getTitle() == null) return 0;
			return lhs.getTitle().compareTo(rhs.getTitle());
		}
	}

	private static class ComparatorByArtist implements Comparator<Track>{
		@Override
		public int compare(Track lhs, Track rhs) {
			if (lhs.getArtist() == null || rhs.getArtist() == null) return 0;
			return lhs.getArtist().compareTo(rhs.getArtist());
		}
	}

	private static class ComparatorByAlbum implements Comparator<Track>{
		@Override
		public int compare(Track lhs, Track rhs) {
			if (lhs.getAlbum() == null || rhs.getAlbum() == null) return 0;
			return lhs.getAlbum().compareTo(rhs.getAlbum());
		}
	}

	private static class ComparatorByDuration implements Comparator<Track>{
		@Override
		public int compare(Track lhs, Track rhs) {
			return ((Long)lhs.getDuration()).compareTo(rhs.getDuration());
		}
	}
}
