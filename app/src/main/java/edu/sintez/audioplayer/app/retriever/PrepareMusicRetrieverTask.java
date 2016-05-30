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

import android.os.AsyncTask;

/**
 * Asynchronous task that prepares a MusicRetriever. This asynchronous task essentially calls
 * {@link MusicRetriever#prepare()} on a {@link MusicRetriever}, which may take some time to
 * run. Upon finishing, it notifies the indicated {@link MusicRetrieverPreparedListener}.
 */
public class PrepareMusicRetrieverTask extends AsyncTask<Void, Void, Void> {
    private static final String LOG = PrepareMusicRetrieverTask.class.getName();

    private MusicRetriever retriever;
    private MusicRetrieverPreparedListener mrpl;

    public PrepareMusicRetrieverTask(MusicRetriever retriever, MusicRetrieverPreparedListener listener) {
        this.retriever = retriever;
        mrpl = listener;
    }

    @Override
    protected Void doInBackground(Void... arg0) {
        retriever.prepare();
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        mrpl.onMusicRetrieverPrepared();
    }


    public interface MusicRetrieverPreparedListener {
        public void onMusicRetrieverPrepared();
    }
}
