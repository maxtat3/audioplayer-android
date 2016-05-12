/*   
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.sintez.audioplayer.retriever;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import edu.sintez.audioplayer.model.Track;
import edu.sintez.audioplayer.utils.FileChooser;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Retrieves and organizes media to play. Before being used, you must call {@link #prepare()},
 * which will retrieve all of the music on the user's device (by performing a query on a content
 * resolver). After that, it's ready to retrieve a random song, with its title and URI, upon
 * request.
 */
public class MusicRetriever {
    private static final String LOG = MusicRetriever.class.getName();
    private boolean isDebug = false;

    ContentResolver mContentResolver;

    // the items (songs) we have queried
    List<Track> tracks = new ArrayList<Track>();

    Random mRandom = new Random();


    public MusicRetriever(ContentResolver cr) {
        mContentResolver = cr;
    }

    /**
     * Loads music data. This method may take long, so be sure to call it asynchronously without
     * blocking the main thread.
     */
    public void prepare() {
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        if (isDebug) Log.d(LOG, "Querying media...");
        if (isDebug) Log.d(LOG, "URI: " + uri.toString());

        // Perform a query on the content resolver. The URI we're passing specifies that we
        // want to query for all audio media on external storage (e.g. SD card)
        Cursor cur = mContentResolver.query(
		        uri,
		        null,
                MediaStore.Audio.Media.IS_MUSIC + " = 1",
		        null,
		        null
        );
        if (isDebug) Log.d(LOG, "Query finished. " + (cur == null ? "Returned NULL." : "Returned a cursor."));

        if (cur == null) {
            // Query failed...
            if (isDebug) Log.d(LOG, "Failed to retrieve music: cursor is null :-(");
            return;
        }
        if (!cur.moveToFirst()) {
            // Nothing to query. There is no music on the device. How boring.
            if (isDebug) Log.e(LOG, "Failed to move cursor to first row (no query results).");
            return;
        }

        if (isDebug) Log.d(LOG, "Listing...");

        // retrieve the indices of the columns where the ID, title, etc. of the song are
	    int filNameColumn = cur.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
	    int fileSizeColumn = cur.getColumnIndex(MediaStore.MediaColumns.SIZE);
	    int artistColumn = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        int titleColumn = cur.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int albumColumn = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int durationColumn = cur.getColumnIndex(MediaStore.Audio.Media.DURATION);
        int idColumn = cur.getColumnIndex(MediaStore.Audio.Media._ID);

        if (isDebug) Log.d(LOG, "Title column index: " + String.valueOf(titleColumn));
        if (isDebug) Log.d(LOG, "ID column index: " + String.valueOf(titleColumn));

        // add each song to tracks
        do {
            if (isDebug) Log.d(LOG, "ID: " + cur.getString(idColumn) + " Title: " + cur.getString(titleColumn));
	        tracks.add(new Track(
		        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cur.getLong(idColumn)),
		        cur.getString(filNameColumn),
		        FileChooser.roundDouble(cur.getDouble(fileSizeColumn)/1024.0/1024.0),
		        cur.getString(artistColumn),
		        cur.getString(titleColumn),
		        cur.getString(albumColumn),
		        cur.getLong(durationColumn)
	        ));
        } while (cur.moveToNext());
        cur.close();

        if (isDebug) Log.d(LOG, "mus items list size = " + tracks.size());
        if (isDebug) Log.d(LOG, "Done querying media. MusicRetriever is ready.");
    }

    public ContentResolver getContentResolver() {
        return mContentResolver;
    }

    /** Returns a random Track. If there are no items available, returns null. */
    public Track getRandomItem() {
        if (tracks.size() <= 0) return null;
        int number = mRandom.nextInt(tracks.size());
        if(isDebug) Log.d(LOG, "number of returner random song = " + number);
        return tracks.get(number);
    }

    public List<Track> getAllAudioTracks() {
        return tracks;
    }

}
