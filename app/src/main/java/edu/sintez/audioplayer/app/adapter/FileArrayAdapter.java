package edu.sintez.audioplayer.app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import edu.sintez.audioplayer.R;
import edu.sintez.audioplayer.app.model.FileItem;
import edu.sintez.audioplayer.app.model.FileType;

import java.util.List;


public class FileArrayAdapter extends ArrayAdapter<FileItem> {

	private static final String LOG = FileArrayAdapter.class.getName();
	private Context context;
	private int id;
	private List<FileItem> items;
	private View.OnClickListener itemSelectListener;

	public FileArrayAdapter(Context context, int textViewResourceId,
	                        List<FileItem> items, View.OnClickListener itemSelectListener) {
		super(context, textViewResourceId, items);
		this.context = context;
		this.id = textViewResourceId;
		this.items = items;
		this.itemSelectListener = itemSelectListener;
	}

	public FileItem getItem(int i) {
		return items.get(i);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(id, null);

			ViewHolder viewHolder = new ViewHolder();
			viewHolder.chb_item = (CheckBox) convertView.findViewById(R.id.chb_item);
			viewHolder.name = (TextView) convertView.findViewById(R.id.tv_name);
			viewHolder.description = (TextView) convertView.findViewById(R.id.tv_description);
			viewHolder.ivFileFormat = (ImageView) convertView.findViewById(R.id.iv_file_format);

			viewHolder.chb_item.setOnClickListener(itemSelectListener);

			convertView.setTag(viewHolder);
		}

		ViewHolder viewHolder = (ViewHolder) convertView.getTag();
		FileItem item = items.get(position);
		viewHolder.chb_item.setTag(item);
		viewHolder.name.setText(item.getName());

		if (item.getType() == FileType.PARENT_DIR) {
			viewHolder.description.setText(FileType.PARENT_DIR.getDesc());
		} else if (item.getType() == FileType.DIR) {
			viewHolder.description.setText(FileType.DIR.getDesc());
		} else if (item.getType() == FileType.FILE) {
			viewHolder.description.setText("File size " + String.valueOf(item.getSize()) + " MB");
		}

		if (position == 0
			&& (item.getType() == FileType.DIR
			|| item.getType() == FileType.PARENT_DIR)) {
			viewHolder.ivFileFormat.setImageResource(R.mipmap.ic_back);

		} else if (item.getType() == FileType.DIR) {
			viewHolder.ivFileFormat.setImageResource(R.mipmap.ic_folder);

		} else {
			switch (item.getFormat()) {
				case MP3:
					viewHolder.ivFileFormat.setImageResource(R.mipmap.ic_mp3);
					break;
				case FLAC:
					viewHolder.ivFileFormat.setImageResource(R.mipmap.ic_flac);
					break;
				case M4A:
					viewHolder.ivFileFormat.setImageResource(R.mipmap.ic_m4a);
					break;
			}
		}

		return convertView;
	}

	public static class ViewHolder {
		public CheckBox chb_item;
		public TextView name;
		public TextView description;
		public ImageView ivFileFormat;
	}

}
