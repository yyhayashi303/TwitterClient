package local.yhayashi.twitter.common;

import java.util.List;

import local.yhayashi.twitter.activity.R;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class HashtagListAdapter extends ArrayAdapter<String> {
	private LayoutInflater inflater;
	public HashtagListAdapter(Context context, List<String> objects) {
		super(context, 0, objects);
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.hashtag_list_item, null);
		}
		String data = getItem(position);
		((TextView) convertView).setText(data);
		return convertView;
	}
}
