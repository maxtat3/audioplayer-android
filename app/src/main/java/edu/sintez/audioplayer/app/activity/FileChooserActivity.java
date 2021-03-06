package edu.sintez.audioplayer.app.activity;

import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Toast;
import edu.sintez.audioplayer.R;
import edu.sintez.audioplayer.app.adapter.FileArrayAdapter;
import edu.sintez.audioplayer.app.model.FileItem;
import edu.sintez.audioplayer.app.model.FileType;
import edu.sintez.audioplayer.app.model.SupportedAudioFormat;
import edu.sintez.audioplayer.app.model.Track;
import edu.sintez.audioplayer.app.utils.Utilities;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class FileChooserActivity extends ListActivity {

	private static final String LOG = FileChooserActivity.class.getName();
	public static final String SELECTED_FILES_LIST_KEY = FileChooserActivity.class.getName() + "." + "selected_files_list_key";
	/**
	 * Key which FileInfoActivity receive audio track. In this audio track
	 * filled fields: uri, name and size only. So track do not contained audio
	 * meta information and must be received her.
	 *
	 * @see FileInfoActivity
	 * @see Track
	 */
	public static final String FILE_CHOOSER_INFO_FILE_KEY = FileChooserActivity.class.getName() + "." + "file_info";

	private File currDir;
	private FileArrayAdapter adapter;

	private File[] filesAndDirs;
	private List<FileItem> dirs = new ArrayList<FileItem>();
	private List<FileItem> files = new ArrayList<FileItem>();

	private ItemSelectListener itemSelectListener = new ItemSelectListener();
	private ArrayList<FileItem> selFiles = new ArrayList<FileItem>();


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_file_chooser);

		Button btnCancel = (Button) findViewById(R.id.btn_fch_cancel);
		Button btnOk = (Button) findViewById(R.id.btn_fch_ok);
		Button btnSelectAll = (Button) findViewById(R.id.btn_fch_select_all);

		btnCancel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

		btnOk.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!selFiles.isEmpty()) {
					setResult(RESULT_OK, new Intent().putParcelableArrayListExtra(SELECTED_FILES_LIST_KEY, selFiles));
					finish();
				} else {
					Toast.makeText(getApplicationContext(), "No files are selected !", Toast.LENGTH_SHORT).show();
				}
			}
		});

		btnSelectAll.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

			}
		});

		currDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
		fill(currDir);
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
					dirs.add(new FileItem(
						FileType.DIR,
						ff.getName(),
						ff.getAbsolutePath()
					));
				} else {
					SupportedAudioFormat detectedFormat = checkFileFormat(ff.getName());
					if (detectedFormat != SupportedAudioFormat.NOT_DEFINED) {
						files.add(new FileItem(
							FileType.FILE,
							ff.getName(),
							ff.getAbsolutePath(),
							Utilities.roundDouble(ff.length()/1024.0/1024.0),
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
		dirs.add(0, new FileItem(FileType.PARENT_DIR, "...", f.getParent()));
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

	@Override
	protected void onListItemClick(ListView lv, View v, int pos, long id) {
		super.onListItemClick(lv, v, pos, id);
		FileItem item = adapter.getItem(pos);
		if (item.getType() == FileType.DIR || item.getType() == FileType.PARENT_DIR) {
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

			if (chbItem.isChecked())  fileActions(item, FileAction.ADD);
			else fileActions(item, FileAction.REMOVE);
		}
	}

	private void fileActions(FileItem item, FileAction action) {
		boolean isDir = item.getType() == FileType.DIR;
		switch (action) {
			case ADD:
				if (isDir) extractFilesOnDir(item, action);
				else selFiles.add(item);
				break;

			case REMOVE:
				if (isDir) extractFilesOnDir(item, action);
				selFiles.remove(item);
				break;
		}
	}

	private void extractFilesOnDir(FileItem item, FileAction action) {
		File[] filesInDir = new File(item.getPath()).listFiles();
		for (File f : filesInDir) {
			if (!f.isDirectory()) {

				if (action == FileAction.ADD) {
					FileItem fi = createFileItem(f);
					if (fi != null) selFiles.add(fi);
				} else if (action == FileAction.REMOVE) {
					selFiles.remove(extractFileItemFromPath(f.getAbsolutePath()));
				}

			}
		}

	}

	private FileItem createFileItem(File f) {
		FileItem fi = null;
		SupportedAudioFormat detectedFormat = checkFileFormat(f.getName());
		if (detectedFormat != SupportedAudioFormat.NOT_DEFINED) {
			fi = new FileItem(
				FileType.FILE,
				f.getName(),
				f.getAbsolutePath(),
				Utilities.roundDouble(f.length() / 1024.0 / 1024.0),
				detectedFormat
			);
		}
		return fi;
	}

	private FileItem extractFileItemFromPath(String absPath) {
		for (FileItem selFile : selFiles) {
			if (selFile.getPath().equals(absPath)) {
				return selFile;
			}
		}
		return null;
	}

	private enum FileAction {
		ADD,
		REMOVE
	}

	/**
	 * Handel when file click action.
	 *
	 * @param item clicked file item
	 */
	private void onFileClick(FileItem item) {
		Track track = new Track();
		track.setUri(Uri.parse(item.getPath()));
		track.setFileName(item.getName());
		track.setFileSize(item.getSize());
		Bundle bundle = new Bundle();
		bundle.putParcelable(FILE_CHOOSER_INFO_FILE_KEY, track);
		Intent infoIntent = new Intent(this, FileInfoActivity.class);
		infoIntent.putExtras(bundle);
		startActivity(infoIntent);
	}
}