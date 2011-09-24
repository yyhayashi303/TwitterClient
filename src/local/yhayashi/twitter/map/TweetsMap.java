package local.yhayashi.twitter.map;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import local.yhayashi.twitter.activity.R;
import local.yhayashi.twitter.activity.TweetDetail;
import local.yhayashi.twitter.common.ImageCache;
import local.yhayashi.twitter.db.TweetsInfo;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

public class TweetsMap extends MapActivity {
	/** インテントパラメータ 住所 */
	public static final String PARAM_ADDRESS = "address";
	/** インテントパラメータ 緯度 */
	public static final String PARAM_LATITUDE = "latitude";
	/** インテントパラメータ 経度 */
	public static final String PARAM_LONGITUDE = "longitude";
	/** インテントパラメータ TweetsInfo */
	public static final String PARAM_TWEETS_INFO = "tweets_info";
	/** 地図の初期ズーム設定 */
	private static final int MAP_ZOOM = 15;
	/** タイトル */
	private TextView title;
	/** ロケーションマネージャ */
	private LocationManager locationManager;
	/** ロケーション */
	private Location location;
	/** ジオコーダ */
	private Geocoder geocoder;
	/** マップコントローラー */
	private MapController controller;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// カスタムタイトル使用
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.tweet_map);
		// タイトルレイアウト設定
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.tweet_map_title);

		// ロケーションマネージャー
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		String provider = locationManager.getBestProvider(new Criteria(), true);
		geocoder = new Geocoder(this, Locale.JAPAN);

		Bundle bundle = getIntent().getExtras();
		String address = bundle.getString(PARAM_ADDRESS);
		double latitude = bundle.getDouble(PARAM_LATITUDE);
		double longitude = bundle.getDouble(PARAM_LONGITUDE);

		// タイトル
		title = (TextView) findViewById(R.id.title_tweet_map);
		if (address != null) {
			title.setText(address);
		} else {
			location = locationManager.getLastKnownLocation(provider);
			new GeoCodeTask().execute(location);
			latitude = location.getLatitude();
			longitude = location.getLongitude();
		}

		MapView mapView = (MapView) findViewById(R.id.map);
		// ツイート情報取得
		@SuppressWarnings("unchecked")
		ArrayList<TweetsInfo> tweetsList = (ArrayList<TweetsInfo>) bundle.getSerializable(PARAM_TWEETS_INFO);
		for (TweetsInfo tweets : tweetsList) {
			Bitmap icon = ImageCache.getImage(tweets.getProfileImageURL());
			TweetsItemizedOverlay tweetsOverlay =
				new TweetsItemizedOverlay(new BitmapDrawable(icon), tweets);
			mapView.getOverlays().add(tweetsOverlay);
		}
		controller = mapView.getController();
		controller.setZoom(MAP_ZOOM);
		controller.animateTo(new GeoPoint((int) (latitude * 1E6), (int) (longitude * 1E6)));
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	/**
	 * Tweets情報を地図上に表示するためのオーバーレイ
	 */
	private class TweetsItemizedOverlay extends ItemizedOverlay<TweetsOverlayItem> {
		/** Tweets情報 */
		private TweetsInfo tweets;
		/** 位置情報 */
		private GeoPoint geoPoint;
		/** オーバーレイするアイテム */
		private TweetsOverlayItem tweetsItem;

		public TweetsItemizedOverlay(Drawable defaultMarker, TweetsInfo tweets) {
			super(boundCenterBottom(defaultMarker));
			this.tweets = tweets;
			this.geoPoint = new GeoPoint(
					(int) (tweets.getLatitude() * 1E6),
					(int) (tweets.getLongitude()* 1E6));
			this.tweetsItem = new TweetsOverlayItem(geoPoint);
			populate();
		}

		@Override
		protected TweetsOverlayItem createItem(int i) {
			return tweetsItem;
		}

		@Override
		public int size() {
			return 1;
		}

		/**
		 * タップされた場合詳細ページを表示
		 */
		@Override
		protected boolean onTap(int index) {
			Intent intent = new Intent(TweetsMap.this, TweetDetail.class);
			intent.putExtra(TweetDetail.PARAM_TWEET_INFO, tweets);
			startActivity(intent);
			return super.onTap(index);
		}
	}
	/**
	 * Tweets情報オーバーレイアイテム
	 */
	class TweetsOverlayItem extends OverlayItem {
		public TweetsOverlayItem(GeoPoint point){
			super(point, "", "");
		}
	}

	private class GeoCodeTask extends AsyncTask<Location, Integer, String> {
		@Override
		protected String doInBackground(Location ... locations) {
			try {
				List<Address> addressList = geocoder.getFromLocation(
						locations[0].getLatitude(), locations[0].getLongitude(), 1);
				if (addressList.size() != 0) {
					return addressList.get(0).getAddressLine(1);
				}
			} catch (IOException e) {
			}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			if (result != null) {
				title.setText(getString(R.string.title_nearby, result));
			} else {
				// ジオコード失敗
				title.setText(getString(R.string.title_nearby, ""));
			}
		}
	}
}

