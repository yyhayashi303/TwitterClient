package local.yhayashi.twitter.common;

import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.RejectedExecutionException;

import local.yhayashi.twitter.activity.R;
import local.yhayashi.twitter.db.TweetsInfo;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class TwitterListAdapter extends ArrayAdapter<TweetsInfo> {
	/** レイアウトインフレータ */
	private LayoutInflater inflater;
	/** ジオコーダ */
	private Geocoder geocoder;
	private DateFormat timeFormat;
	/**
	 * コンストラクタ
	 * @param context コンテキスト
	 * @param list TweetsInfoのリスト
	 */
	public TwitterListAdapter(Context context, List<TweetsInfo> list) {
		super(context, 0, list);
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		geocoder = new Geocoder(context, Locale.JAPAN);
		timeFormat = android.text.format.DateFormat.getTimeFormat(context);
	}

	/**
	 * Viewホルダ
	 */
	class ViewHolder {
		TextView nameView;
		TextView timeView;
		TextView textView;
		TextView addressView;
		ImageView iconView;
	}
	// 1行ごとのビューを生成する
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.item_row, null);
			holder = new ViewHolder();
			holder.nameView = (TextView) convertView.findViewById(R.id.item_screen_name);
			holder.timeView = (TextView) convertView.findViewById(R.id.item_time);
			holder.textView = (TextView) convertView.findViewById(R.id.item_tweets);
			holder.addressView = (TextView) convertView.findViewById(R.id.item_address);
			holder.iconView = (ImageView) convertView.findViewById(R.id.item_icon);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		// 現在参照しているリストの位置からItemを取得する
		TweetsInfo tweetsInfo = this.getItem(position);
		if (tweetsInfo != null) {
			// ユーザー名
			holder.nameView.setText(tweetsInfo.getScreenName());
			// タグにステータスID設定
			holder.nameView.setTag(tweetsInfo.getStatusId());
			log(position + ":" + tweetsInfo.getScreenName() + "," + tweetsInfo.getUserId());
			// アイコン設定
			setBitmap(holder.iconView, tweetsInfo);
			// 時刻
			holder.timeView.setText(timeFormat.format(tweetsInfo.getCreatedAt()));
			// ツイート
			holder.textView.setText(tweetsInfo.getText());
			// 住所
			setAddress(holder.addressView, tweetsInfo);
		}
		return convertView;
	}

	/**
	 * アイコン画像のをViewに設定します
	 * @param iconView アイコンを表示するImageView
	 * @param tweetsInfo
	 */
	private void setBitmap(ImageView iconView, TweetsInfo tweetsInfo) {
		String url = tweetsInfo.getProfileImageURL();
		if (url == null) {
			return;
		}
		iconView.setVisibility(View.INVISIBLE);
		log(url);
		// メモリにキャッシュされているものを使用
		Bitmap bitmap = ImageCache.getImage(url);
		if (bitmap != null) {
			log("メモリ:" + tweetsInfo.getScreenName());
			iconView.setImageBitmap(bitmap);
			iconView.setVisibility(View.VISIBLE);
			return;
		}
		// DBキャッシュがある場合はそれを使用
		byte[] bytes = tweetsInfo.getBitmap();
		if(bytes != null && 0 < bytes.length) {
			bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
			ImageCache.setImage(url, bitmap);
			iconView.setImageBitmap(bitmap);
			iconView.setVisibility(View.VISIBLE);
			log("DB:" + tweetsInfo.getScreenName());
			return;
		}
		log("非同期:" + tweetsInfo.getScreenName());
		// 非同期タスクで取得し設定
		iconView.setTag(url);
		try {
			new IconSetTask(iconView).execute(url);
		} catch (RejectedExecutionException e) {
		}
	}
	private void log(Object obj) {
		Log.i("TwitterListAdapter", obj.toString());
	}

	/**
	 * 住所設定
	 * @param addressView 住所を設定するTextView
	 * @param tweetsInfo
	 */
	private void setAddress(TextView addressView, TweetsInfo tweetsInfo) {
		double latitude = tweetsInfo.getLatitude();
		double longitude = tweetsInfo.getLongitude();
		addressView.setVisibility(View.GONE);
		if (latitude != -1 && longitude != -1) {
			String key = latitude + "," + longitude;
			String addressData = AddressCache.getAddress(key);
			if (addressData != null) {
				addressView.setText(addressData);
				addressView.setVisibility(View.VISIBLE);
			} else {
				addressView.setTag(key);
				new AddressSetTask(addressView, geocoder).execute(latitude, longitude);
			}
		}
	}

	private class IconSetTask extends AsyncTask<String, Integer, Bitmap> {
		private final ImageView imageView;
		private final String tag;
		public IconSetTask(final ImageView imageView) {
			this.imageView = imageView;
			this.tag = imageView.getTag().toString();
		}

		@Override
		protected Bitmap doInBackground(String... urls) {
			Bitmap bitmap = null;
			try {
				URL url = new URL(urls[0]);
				bitmap = BitmapFactory.decodeStream(url.openStream());
				ImageCache.setImage(urls[0], bitmap);
			} catch (IOException e) {
			}
			return bitmap;
		}

		@Override
		protected void onPostExecute(Bitmap icon) {
			if (icon != null
					&& tag.equals(imageView.getTag())
					&& imageView.getVisibility() == View.INVISIBLE) {
				imageView.setImageBitmap(icon);
				imageView.setVisibility(View.VISIBLE);
			}
		}
	}

	private class AddressSetTask extends AsyncTask<Double, Integer, List<Address>> {
		private TextView addressView;
		private String tag;
		public AddressSetTask(TextView addressView, Geocoder geocoder) {
			this.addressView = addressView;
			this.tag = addressView.getTag().toString();
		}
		@Override
		protected List<Address> doInBackground(Double ... locations) {
			try {
				return geocoder.getFromLocation(locations[0], locations[1], 1);
			} catch (IOException e) {
			}
			return null;
		}
		@Override
		protected void onPostExecute(List<Address> result) {
			if (result == null || result.size() == 0) {
				return;
			}
			if (tag.equals(addressView.getTag())
					&& addressView.getVisibility() == View.GONE) {
				String addressData = result.get(0).getAddressLine(1);
				addressView.setText(addressData);
				addressView.setVisibility(View.VISIBLE);
				AddressCache.setAddress(tag, addressData);
			}
		}

	}
}
