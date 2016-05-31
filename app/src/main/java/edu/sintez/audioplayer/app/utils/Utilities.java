package edu.sintez.audioplayer.app.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

/**
 * Collection utilitarians methods used are general in this app.
 */
public class Utilities {


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
}
