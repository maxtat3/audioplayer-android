package edu.sintez.audioplayer.utils;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Toast;
import edu.sintez.audioplayer.R;
import edu.sintez.audioplayer.adapter.FileArrayAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class FileChooser extends ListActivity {

	private static final String LOG = FileChooser.class.getName();
	public static final String SELECTED_FILES_KEY = FileChooser.class.getName() + "." + "selected_files_key";
	public static final String PARENT_DIR_TXT = "Parent Directory";
	public static final String FOLDER_TXT = "Folder";
	private File currDir;
	private FileArrayAdapter adapter;

	private File[] filesAndDirs;
	private List<FileItem> dirs = new ArrayList<FileItem>();
	private List<FileItem> files = new ArrayList<FileItem>();

	private ItemSelectListener itemSelectListener = new ItemSelectListener();
	private ArrayList<String> selFiles = new ArrayList<String>();


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		currDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
		fill(currDir);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_files_sel, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.mi_ok:
				setResult(RESULT_OK, new Intent().putStringArrayListExtra(SELECTED_FILES_KEY, selFiles));
				finish();
				break;
			case R.id.mi_cancel:
				finish();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Filled file chooser list view for user navigates in folders
	 * and selected music files.
	 *
	 * @param f root directory for navigates. Files and directories
	 *          which are located them displayed in file chooser list view.
	 */
	private void fill(File f) {
		setTitle("Current Dir: " + f.getName());

		if (filesAndDirs != null && filesAndDirs.length > 0) {
			filesAndDirs = null;
		}
		if (dirs != null && !dirs.isEmpty()) {
			dirs.clear();
		}
		if (files != null && !files.isEmpty()) {
			files.clear();
		}
		if (selFiles != null && !selFiles.isEmpty()) {
			selFiles.clear();
		}

		filesAndDirs = f.listFiles();
		dirs = new ArrayList<FileItem>();
		files = new ArrayList<FileItem>();
		try {
			for (File ff : filesAndDirs) {
				if (ff.isDirectory()) {
					dirs.add(new FileItem(ff.getName(), FOLDER_TXT, ff.getAbsolutePath()));
				} else {
					SupportedAudioFormat detectedFormat = checkFileFormat(ff.getName());
					if (detectedFormat != SupportedAudioFormat.NOT_DEFINED) {
						files.add(new FileItem(
							ff.getName(),
							"File Size: " + roundDouble(ff.length()/1024.0/1024.0) + " MB",
							ff.getAbsolutePath(),
							detectedFormat
						));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		Collections.sort(dirs);
		Collections.sort(files);
		dirs.addAll(files);
		if (!f.getName().equalsIgnoreCase("sdcard"))
			dirs.add(0, new FileItem("...", PARENT_DIR_TXT, f.getParent()));
		adapter = new FileArrayAdapter(this, R.layout.pattern_file_or_dir_item, dirs, itemSelectListener);
		setListAdapter(adapter);
	}

	/**
	 * Find audio files only. Other files is ignored.
	 * If audio format is supporting - returned this file format enum item.
	 *
	 * @param fileName full file name with extension
	 * @return file format enum item if this file included in group supporting audio formats, otherwise
	 *          returned {@link SupportedAudioFormat#NOT_DEFINED}.
	 * @see SupportedAudioFormat
	 */
	private SupportedAudioFormat checkFileFormat(String fileName) {
		String[] nameParts = fileName.split("\\.");
		String ext = nameParts[nameParts.length - 1];
		for (SupportedAudioFormat supportedFormats : SupportedAudioFormat.class.getEnumConstants()) {
			if (ext.equalsIgnoreCase(supportedFormats.getExt())) {
				return supportedFormats;
			}
		}
		return SupportedAudioFormat.NOT_DEFINED;
	}

	/**
	 * Rounding double number of error rounding machine to 2 sings after decimal point.
	 *
	 * @param dig number
	 * @return rounded number
	 */
	private double roundDouble(double dig) {
		final int SINGS = 100; //if this num = 1000 -> rounded num = x.xxx
		int iVal = (int) ( dig * SINGS );
		double dVal = dig * SINGS;
		if ( dVal - iVal >= 0.5 ) {
			iVal += 1;
		}
		dVal = (double) iVal;
		return dVal/SINGS;
	}

	@Override
	protected void onListItemClick(ListView lv, View v, int pos, long id) {
		super.onListItemClick(lv, v, pos, id);
		FileItem item = adapter.getItem(pos);
		if (item.getData().equalsIgnoreCase(FOLDER_TXT) || item.getData().equalsIgnoreCase(PARENT_DIR_TXT)) {
			currDir = new File(item.getPath());
			fill(currDir);
		} else {
			onFileClick(item);
		}
	}


	/**
	 * Listener handled checkbox file click.
	 * When file checked he add in selected files storage.
	 */
	private class ItemSelectListener implements View.OnClickListener {
		@Override
		public void onClick(View view) {
			CheckBox chbItem = (CheckBox) view;
			FileItem item = (FileItem) chbItem.getTag();

			if (chbItem.isChecked()) {
				selFiles.add(item.getPath());

			} else if (selFiles.contains(item.getPath())) {
				selFiles.remove(item.getPath());
			}

		}
	}

	/**
	 * Handel file click action.
	 *
	 * @param item clicked file item
	 */
	private void onFileClick(FileItem item) {
		Toast.makeText(this, "File Clicked: " + item.getName(), Toast.LENGTH_SHORT).show();
		Log.d(LOG, "path to file = " + item.getPath());
	}
}