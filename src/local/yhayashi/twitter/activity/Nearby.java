package local.yhayashi.twitter.activity;

import static local.yhayashi.twitter.activity.TweetMain.PARAM_REPLY_NAME;
import static local.yhayashi.twitter.activity.TweetMain.PARAM_SEND_TYPE;
import static local.yhayashi.twitter.activity.TweetMain.TYPE_REPLY;
import static local.yhayashi.twitter.map.TweetsMap.PARAM_ADDRESS;
import static local.yhayashi.twitter.map.TweetsMap.PARAM_LATITUDE;
import static local.yhayashi.twitter.map.TweetsMap.PARAM_LONGITUDE;
import static local.yhayashi.twitter.map.TweetsMap.PARAM_TWEETS_INFO;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import local.yhayashi.twitter.common.ImageCache;
import local.yhayashi.twitter.common.TwitterListAdapter;
import local.yhayashi.twitter.db.TweetsInfo;
import local.yhayashi.twitter.db.TweetsInfoFactory;
import local.yhayashi.twitter.map.TweetsMap;
import local.yhayashi.twitter.task.ImagePreLoadTask2;
import twitter4j.GeoLocation;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Tweet;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class Nearby extends Activity implements View.OnClickListener {
	/** ダイアログ ロケーションプロバイダーが無い場合 */
	private static final int DIALOG_PROVIDER_NOTFOUND = 1;
	/** ダイアログ 現在地取得失敗 */
	private static final int DIALOG_UPDATE_LOCATION_FAILED = 2;
	/** 現在地読み込みタイムアウト  GPS*/
	private static long LOCATION_LOAD_TIMEOUT_GPS = 60 * 1000L;
	/** 現在地読み込みタイムアウト その他プロバイダ */
	private static long LOCATION_LOAD_TIMEOUT_OTHER = 30 * 1000L;
	/** 位置情報の変更を通知する間隔（目安） */
	private static final int MIN_TIME = 15000;
	/** 位置情報の変更を通知する移動間隔 */
	private static final int MIN_DISTANCE = 0;
	/** 近くのツイートを取得する際の範囲（km） */
	private static final int LOCATION_RANGE = 3;
	/** コンテキストメニュー：返信 */
	private static final int MENU_REPLY = Menu.FIRST + 1;
	/** コンテキストメニュー：共有 */
	private static final int MENU_SHARE = Menu.FIRST + 2;
	/** １ページの最大読み込み数 */
	private static final int MAX_COUNT_PER_PAGE = 200;

	/** 位置情報取得経過時間 */
	private long time;
	/** 位置情報取得タイムアウト */
	private long locationLoadTimeout;
	/** 位置情報管理 */
	private LocationManager locationManager;
	/** 位置情報イベントリスナ */
	private LocationListener locationListener;
	/** ジオコーダ */
	private Geocoder geocoder;
	/** 位置情報 */
	private Location location;
	/** カスタムタイトル */
	private TextView title;
	/** 地図表示ボタン */
	private Button mapButton;
	/** 更新ボタン */
	private Button refreshButton;
	/** 現在地更新ボタン */
	private Button updateLocationButton;
	/** 近くのツイート取得タスク */
	private NearlyTweetsFindTask task;
	/** 現在地読み込みタイムアウト判定タイマー */
	private Timer locationTimer;
	/** タイマーのチェック間隔 */
	private long timerCheckInterval = 1000L;
	/** 現在地更新プログレスダイアログ */
	private ProgressDialog updateLocationDialog;
	/** ハッシュタグツイートリスト */
	private ListView listView;
	/** ツイッターアダプタ */
	private TwitterListAdapter adapter;
	/** ツイッターオブジェクト */
	private Twitter twitter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// カスタムタイトル使用
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.nearby);
		// タイトルレイアウト設定
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.nearby_title);

		// タイトル
		title = (TextView) findViewById(R.id.title_nearby);
		// 現在地特定時ダイアログ
		updateLocationDialog = new ProgressDialog(this);
		updateLocationDialog.setMessage(getString(R.string.location_loading));

		// Twitterオブジェクト取得
		twitter = ((TwitterClientBean) getApplication()).getTwitter();

		// 現在地更新ボタン
		updateLocationButton = (Button) findViewById(R.id.btn_update_location);
		updateLocationButton.setOnClickListener(this);
		// 地図ボタン
		mapButton = (Button) findViewById(R.id.btn_map);
		mapButton.setOnClickListener(this);
		// 更新ボタン
		refreshButton = (Button) findViewById(R.id.btn_refresh);
		refreshButton.setOnClickListener(this);

		adapter = new TwitterListAdapter(this, new ArrayList<TweetsInfo>());
		listView = (ListView) findViewById(R.id.nearby_list);
		listView.setAdapter(adapter);
		// 高速スクロール
		listView.setFastScrollEnabled(true);
		// クリックされたアイテムの詳細ページへ遷移
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Intent intent = new Intent(Nearby.this, TweetDetail.class);
				intent.putExtra(TweetDetail.PARAM_TWEET_INFO, adapter.getItem(position));
				startActivity(intent);
			}
		});

		// ロケーションマネージャー
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		String provider = locationManager.getBestProvider(new Criteria(), true);
		geocoder = new Geocoder(this, Locale.JAPAN);
		location = locationManager.getLastKnownLocation(provider);
		if (location == null) {
			updateLocation();
		} else {
			setTitleLocation(location);
		}

		// コンテキストメニュー
		registerForContextMenu(listView);

		// 近くのツイート検索
		findNearlyTweets();
	}

	@Override
	protected void onPause() {
		super.onPause();
		// 終了していないタスクをキャンセル
		cancelTask();
		// 位置情報更新中の場合は終了
		stopUpdateLocation();
		// 全ボタン有効化
		allButtonEnabled(true);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_update_location:
			// 動いているタスクをキャンセル
			cancelTask();
			updateLocation();
			break;
		case R.id.btn_map:
			// 地図表示
			showMap();
			break;
		case R.id.btn_refresh:
			findNearlyTweets();
			break;
		}
	}

	/**
	 * 
	 */
	private void showMap() {
		int size = adapter.getCount();
		ArrayList<TweetsInfo> tweetsList = new ArrayList<TweetsInfo>();
		// 位置情報があるもののみ渡す
		for (int i = 0; i < size; i++) {
			TweetsInfo tweets = adapter.getItem(i);
			if (tweets.getLatitude() != -1 && tweets.getLongitude() != -1) {
				tweetsList.add(tweets);
			}
		}
		Intent intent = new Intent(this, TweetsMap.class);
		intent.putExtra(PARAM_TWEETS_INFO, tweetsList);
		intent.putExtra(PARAM_ADDRESS, title.getText());
		intent.putExtra(PARAM_LATITUDE, location.getLatitude());
		intent.putExtra(PARAM_LONGITUDE, location.getLongitude());
		startActivity(intent);
	}
	/**
	 * タスクが実行中の場合はキャンセルします。
	 */
	private void cancelTask() {
		if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
			task.cancel(true);
		}
	}

	/**
	 * 近くのツイートを検索
	 */
	private void findNearlyTweets() {
//		adapter.clear();
		Query query = new Query();
		query.setRpp(MAX_COUNT_PER_PAGE);
		query.setGeoCode(
				new GeoLocation(location.getLatitude(), location.getLongitude()),
				LOCATION_RANGE,
				Query.KILOMETERS);
		task = new NearlyTweetsFindTask();
		task.execute(query);
	}

	/**
	 * 位置情報設定
	 * @param location 位置情報
	 */
	private void setLocation(Location location) {
		this.location = location;
		setTitleLocation(location);
	}

	/**
	 * タイトルに現在地の住所を設定
	 * @param location 位置情報
	 */
	private void setTitleLocation(Location location) {
		new GeoCodeTask().execute(location);
	}

	private void allButtonEnabled(boolean enable) {
		// 現在地更新ボタン無効化
		updateLocationButton.setEnabled(enable);
		// 地図ボタン無効化
		mapButton.setEnabled(enable);
		// 更新ボタン無効化
		refreshButton.setEnabled(enable);
	}

	/**
	 * 現在地更新
	 */
	private void updateLocation() {
		String provider;
		// GPSが有効な場合はGPSを使う
		if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			provider = LocationManager.GPS_PROVIDER;
			locationLoadTimeout = LOCATION_LOAD_TIMEOUT_GPS;
		} else {
			Criteria criteria = new Criteria();
			// 進行方向不要
			criteria.setBearingRequired(false);
			// 速度不要
			criteria.setSpeedRequired(false);
			// 高度不要
			criteria.setAltitudeRequired(false);
			provider = locationManager.getBestProvider(criteria, true);
			locationLoadTimeout = LOCATION_LOAD_TIMEOUT_OTHER;
		}
		if (provider == null) {
			// プロバイダが存在しない場合は設定を促す
			showDialog(DIALOG_PROVIDER_NOTFOUND);
			return;
		}
		// ダイアログ表示
		updateLocationDialog.show();
		// すべてのボタンを無効化
		allButtonEnabled(false);

		// ロケーション取得のタイムアウトを検知するタイマ
		locationTimer = new Timer(true);
		time = 0L;
		final Handler handler = new Handler();
		locationTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				handler.post(new Runnable() {
					@Override
					public void run() {
						if (locationLoadTimeout <= time) {
							// 位置情報更新終了
							stopUpdateLocation();
							// 全ボタンを有効化
							allButtonEnabled(true);
							// 位置情報更新失敗ダイアログ表示
							showDialog(DIALOG_UPDATE_LOCATION_FAILED);
						}
						time += timerCheckInterval;
					}
				});
			}
		}, 0L, timerCheckInterval);
		// ロケーションリスナー生成
		locationListener = new LocationListener() {
			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {}
			@Override
			public void onProviderEnabled(String provider) {}
			@Override
			public void onProviderDisabled(String provider) {}
			@Override
			public void onLocationChanged(Location location) {
				// 位置情報更新終了
				stopUpdateLocation();
				// 全ボタン有効化
				allButtonEnabled(true);
				// 取得した位置情報設定
				setLocation(location);
				// 検索
				findNearlyTweets();
			}
		};
		// 現在地更新
		locationManager.requestLocationUpdates(
				provider, MIN_TIME, MIN_DISTANCE, locationListener);
	}

	/**
	 * 位置情報更新終了
	 */
	private void stopUpdateLocation() {
		// ダイアログ消去
		updateLocationDialog.dismiss();
		if (locationTimer != null) {
			locationTimer.cancel();
			locationTimer.purge();
			locationTimer = null;
		}
		if (locationListener != null) {
			locationManager.removeUpdates(locationListener);
			locationListener = null;
		}
	}

	/**
	 * コンテキストメニュー
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		menu.setHeaderTitle(getString(R.string.context_menu_title));
		menu.add(Menu.NONE, MENU_REPLY, Menu.NONE, getString(R.string.context_menu_reply));
		menu.add(Menu.NONE, MENU_SHARE, Menu.NONE, getString(R.string.context_menu_share));
	}

	/**
	 * コンテキストメニューが選択された際の処理
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		int id = item.getItemId();
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		TweetsInfo tweets = adapter.getItem(listView.getPositionForView(info.targetView));
		switch (id) {
		case MENU_REPLY:
			Intent replyIntent = new Intent(this, TweetMain.class);
			replyIntent.putExtra(PARAM_SEND_TYPE, TYPE_REPLY);
			replyIntent.putExtra(PARAM_REPLY_NAME, tweets.getScreenName());
			startActivity(replyIntent);
			return true;
		case MENU_SHARE:
			Intent shareIntent = new Intent();
			shareIntent.setAction(Intent.ACTION_SEND);
			shareIntent.setType("text/plain");
			shareIntent.putExtra(Intent.EXTRA_TEXT,
					getString(R.string.share_text, tweets.getScreenName(), tweets.getText()));
			startActivity(shareIntent);
			return true;
		}
		return super.onContextItemSelected(item);
	}

	/**
	 * アラートダイアログ生成
	 */
	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		switch (id) {
		case DIALOG_PROVIDER_NOTFOUND:
			return new AlertDialog.Builder(this)
			.setTitle(getString(R.string.dialog_location_settings_tite))
			.setMessage(getString(R.string.dialog_location_settings_message))
			.setPositiveButton(getString(R.string.dialog_location_settings_positive),
					new DialogInterface.OnClickListener() {
				@Override
				public void onClick(final DialogInterface dialog, final int which) {
					// 端末の位置情報設定画面へ遷移
					try {
						startActivity(new Intent("android.settings.LOCATION_SOURCE_SETTINGS"));
						updateLocation();
					} catch (final ActivityNotFoundException e) {
						// ignore
					}
				}
			})
			.setNegativeButton(getString(R.string.dialog_location_settings_negative),
					new DialogInterface.OnClickListener() {
				@Override public void onClick(final DialogInterface dialog, final int which) {}
			})
			.create();
		case DIALOG_UPDATE_LOCATION_FAILED:
			return new AlertDialog.Builder(this)
			.setMessage(getString(R.string.location_load_failed))
			.setPositiveButton(getString(R.string.dialog_update_location_failed_positive),
					new DialogInterface.OnClickListener() {
				@Override
				public void onClick(final DialogInterface dialog, final int which) {
					updateLocation();
				}
			})
			.setNegativeButton(getString(R.string.dialog_update_location_failed_negative),
					new DialogInterface.OnClickListener() {
				@Override public void onClick(final DialogInterface dialog, final int which) {
				}
			})
			.create();
		default:
			return null;
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

	/**
	 * 近くのツイートを取得する非同期タスク
	 */
	private class NearlyTweetsFindTask extends AsyncTask<Query, Integer, List<TweetsInfo>> {
		private ProgressDialog progress;

		@Override
		protected List<TweetsInfo> doInBackground(Query ... querys) {
			try {
				List<TweetsInfo> list = new ArrayList<TweetsInfo>();
				List<URL> urlList = new ArrayList<URL>();
				QueryResult result = twitter.search(querys[0]);
				for (Tweet tweet : result.getTweets()) {
					if (isCancelled()) {
						return null;
					}
					list.add(TweetsInfoFactory.createTweetsInfo(tweet));
					String imageUrl = tweet.getProfileImageUrl();
					if (ImageCache.getImage(imageUrl) == null) {
						try {
							urlList.add(new URL(imageUrl));
						} catch (MalformedURLException e) {
						}
					}
				}
				if (0 < urlList.size()) {
					new ImagePreLoadTask2().execute(urlList.toArray(new URL[]{}));
				}
				return list;
			} catch (TwitterException e) {
			}
			return null;
		}
		@Override
		protected void onPreExecute() {
			// 地図ボタン無効化
			mapButton.setEnabled(false);
			// 更新ボタン無効化
			refreshButton.setEnabled(false);
			// プログレス表示
			progress = new ProgressDialog(Nearby.this);
			progress.setMessage(getString(R.string.load_nearly_tweet));
			progress.show();
		}

		@Override
		protected void onPostExecute(List<TweetsInfo> list) {
			// プログレス消去
			progress.dismiss();
			// 更新ボタン有効化
			refreshButton.setEnabled(true);
			// 地図ボタン有効化
			mapButton.setEnabled(true);
			if (list != null) {
				// 取得したデータをアダプタに設定
				int size = list.size();
				for (int i = size -1; 0 <= i; i--) {
					adapter.insert(list.get(i), 0);
				}
				adapter.notifyDataSetChanged();
			}
		}

		@Override
		protected void onCancelled() {
			progress.dismiss();
		}
	}
}
