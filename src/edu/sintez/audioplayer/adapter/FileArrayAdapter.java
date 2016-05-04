package edu.sintez.audioplayer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import edu.sintez.audioplayer.R;
import edu.sintez.audioplayer.utils.FileItem;

import java.util.List;


public class FileArrayAdapter extends ArrayAdapter<FileItem> {

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

			viewHolder.chb_item.setOnClickListener(itemSelectListener);

			convertView.setTag(viewHolder);
		}

		ViewHolder viewHolder = (ViewHolder) convertView.getTag();
		FileItem item = items.get(position);
		viewHolder.chb_item.setTag(item);
		viewHolder.name.setText(item.getName());
		viewHolder.description.setText(item.getData());

		return convertView;
	}

	public static class ViewHolder {
		public CheckBox chb_item;
		public TextView name;
		public TextView description;
	}

}
