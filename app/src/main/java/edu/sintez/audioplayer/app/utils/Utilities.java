package edu.sintez.audioplayer.app.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.List;

/**
 * Collection utilitarians methods used are general in this app.
 */
public class Utilities {

	private static final String LOG = Utilities.class.getName();

	/**
	 * Rounding double number of error rounding machine to 2 sings after decimal point.
	 *
	 * @param dig number
	 * @return rounded number
	 */
	public static double roundDouble(double dig) {
		final int SINGS = 100; //if this num = 1000 -> rounded num = x.xxx
		int iVal = (int) ( dig * SINGS );
		double dVal = dig * SINGS;
		if ( dVal - iVal >= 0.5 ) {
			iVal += 1;
		}
		dVal = (double) iVal;
		return dVal/SINGS;
	}

	/**
	 * Converted Uri path from ContentResolver format to absolute Uri path.
	 *
	 * @param context application context
	 * @param contentUri returned ContentResolver Uri
	 * @return absolute path uri to file
	 * @see android.content.ContentResolver
	 */
	public static String getAbsPathFromURI(Context context, Uri contentUri) {
		Cursor cursor = null;
		try {
			String[] projection = {MediaStore.Images.Media.DATA};
			cursor = context.getContentResolver().query(contentUri, projection, null, null, null);
			int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			cursor.moveToFirst();
			return cursor.getString(column_index);
		} finally {
			if (cursor != null) cursor.close();
		}
	}

	/**
	 * Converted time in milliseconds to format HH:MM:SS and returned time in text representation
	 *
	 * @param millis time in milliseconds
	 * @return time in text representation
	 */
	public static String getTimeText(long millis) {
		StringBuilder buf = new StringBuilder();

		int hours = (int) (millis / (1000 * 60 * 60));
		int minutes = (int) ((millis % (1000 * 60 * 60)) / (1000 * 60));
		int seconds = (int) (((millis % (1000 * 60 * 60)) % (1000 * 60)) / 1000);

		buf
			.append(String.format("%02d", hours))
			.append(":")
			.append(String.format("%02d", minutes))
			.append(":")
			.append(String.format("%02d", seconds));

		return buf.toString();
	}

	/**
	 * Storage for come generated numbers in range [min, max] .
	 */
	private static List<Integer> existRands = new ArrayList<Integer>();

	/**
	 * Counter how to many numbers must be generating.
	 */
	private static int rndNumbersGen;

	/**
	 * This const signals if all numbers are generated in defined [min, max] range.
	 */
	public static final int NO_AVAILABLE_RAND_NUMS = -1;

	/**
	 * Generating random numbers without repeating in determined range
	 * by formula: Min + (int)(Math.random() * ((Max - Min) + 1)) .
	 * <a href="http://allmycircuitz.blogspot.com/2013/09/java.html">Full algorithm description.</a>
	 *
	 * @param min left bound number (min number is included to range)
	 * @param max right bound number (max number is included to range)
	 * @return random number in [min, max] range. -1 code means that all rands are generated.
	 */
	public static int randGen(int min, int max){
		// generating random number
		int rand;
		// flag equals generated number and number which is already exist in storage {@link #existRands}
		boolean isEqual;

		if (existRands.isEmpty()){
			rndNumbersGen = max - min;
			rand = min + (int)(Math.random() * ((max - min) + 1));
			existRands.add(rand);
		} else {
			if (rndNumbersGen == 0){
				existRands.clear();
				return NO_AVAILABLE_RAND_NUMS;
			}
			do {
				isEqual = false;
				rand = min + (int)(Math.random() * ((max - min) + 1));
				for (Integer existRand : existRands) {
					if (rand == existRand) {
						isEqual = true;
						break;
					}
				}
			} while (isEqual);

			rndNumbersGen--;
			existRands.add(rand);
		}

		return rand;
	}

	/**
	 * Reset already exist generated numbers in storage list {@link #existRands}.
	 */
	public static void resetRandGen() {
		existRands.clear();
	}
}
