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

package edu.sintez.audioplayer.app.retriever;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import edu.sintez.audioplayer.app.model.Track;
import edu.sintez.audioplayer.app.utils.Utilities;

import java.util.ArrayList;
import java.util.List;

/**
 * Retrieves and organizes media to play. Before being used, you must call {@link #prepare()},
 * which will retrieve all of the music on the user's device (by performing a query on a content
 * resolver). After that, it's ready to retrieve a random song, with its title and URI, upon
 * request.
 */
public class MusicRetriever {
    private static final String LOG = MusicRetriever.class.getName();

    private ContentResolver contentRes;

	/**
	 * The tracks (songs) we have queried
	 */
    private List<Track> tracks = new ArrayList<Track>();

    public MusicRetriever(ContentResolver cr) {
        contentRes = cr;
    }

    /**
     * Loads music data. This method may take long, so be sure to call it asynchronously without
     * blocking the main thread.
     */
    public void prepare() {
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        // Perform a query on the content resolver. The URI we're passing specifies that we
        // want to query for all audio media on external storage (e.g. SD card)
        Cursor cur = contentRes.query(
		        uri,
		        null,
                MediaStore.Audio.Media.IS_MUSIC + " = 1",
		        null,
		        null
        );

        if (cur == null) {
	        Log.d(LOG, "Failed to retrieve music: cursor is null !");
	        return;
        }
	    if (!cur.moveToFirst()) {
		    Log.d(LOG, "There are no music on the device.");
            return;
        }

	    int filNameColumn = cur.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
	    int fileSizeColumn = cur.getColumnIndex(MediaStore.MediaColumns.SIZE);
	    int artistColumn = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        int titleColumn = cur.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int albumColumn = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int durationColumn = cur.getColumnIndex(MediaStore.Audio.Media.DURATION);
        int idColumn = cur.getColumnIndex(MediaStore.Audio.Media._ID);

        do {
	        tracks.add(new Track(
		        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cur.getLong(idColumn)),
		        cur.getString(filNameColumn),
		        Utilities.roundDouble(cur.getDouble(fileSizeColumn)/1024.0/1024.0),
		        cur.getString(artistColumn),
		        cur.getString(titleColumn),
		        cur.getString(albumColumn),
		        cur.getLong(durationColumn),
		        0
	        ));
        } while (cur.moveToNext());
        cur.close();
    }

    public List<Track> getAllAudioTracks() {
        return tracks;
    }

}
