package local.yhayashi.twitter.activity;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import twitter4j.GeoLocation;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class TweetMain extends Activity implements View.OnClickListener {
	/** インテントパラメータ：返信 */
	public static final String PARAM_REPLY_NAME = "reply_name";
	/** インテントパラメータ：ハッシュタグ */
	public static final String PARAM_TWEET_HASHTAG = "hashtag";
	/** インテントパラメータ：送信種別 */
	public static final String PARAM_SEND_TYPE = "send_type";
	/** 送信種別：返信 */
	public static final int TYPE_REPLY = 1;
	/** 送信種別：ハッシュタグ */
	public static final int TYPE_HASHTAG = 2;

	/** 最大入力文字数 */
	private static final int MAX_LENGTH = 140;
	/** 現在地読み込みタイムアウト  GPS*/
	private static long LOCATION_LOAD_TIMEOUT_GPS = 60 * 1000L;
	/** 現在地読み込みタイムアウト その他プロバイダ */
	private static long LOCATION_LOAD_TIMEOUT_OTHER = 30 * 1000L;
	/** 位置情報の変更を通知する間隔の目安（msec） */
	private static final int MIN_TIME = 15000;
	/** 位置情報の変更を通知する移動間隔 */
	private static final int MIN_DISTANCE = 0;
	/** Preferenceキー：前回入力内容保存用 */
	private static final String PREVIOUSE_INPUT = "previouse_input";
	/** Preferenceキー：返信 */
	private static final String REPLY_INPUT = "reply_input_";
	/** Preferenceキー：ハッシュタグ */
	private static final String HASHTAG_INPUT = "hashtag_input_";
	/** ダイアログ サービス接続失敗 */
	private static final int DIALOG_CONNECTED_FAILED = 0;
	/** ダイアログ ロケーションプロバイダーが無い場合 */
	private static final int DIALOG_PROVIDER_NOTFOUND = 1;
	/** ダイアログ 現在地更新 */
	private static final int DIALOG_UPDATE_LOCATION = 2;

	/** 送信種別 */
	private int sendType;
	/** 送信完了判定 */
	private boolean isCompleted;
	/** 入力内容保存用キー */
	private String preferenceKey;
	/** 位置情報取得経過時間 */
	private long time;
	/** 位置情報取得タイムアウト */
	private long locationLoadTimeout;
	/** 入力フィールド */
	private EditText input;
	/** 送信ボタン */
	private Button sendTweetButton;
	/** 位置情報解除チェックボックス */
	private CheckBox locationOffCheck;
	/** 位置情報表示 */
	private TextView locationView;
	/** 位置情報管理 */
	private LocationManager locationManager;
	/** 位置情報イベントリスナ */
	private LocationListener locationListener;
	/** ジオコーダ */
	private Geocoder geocoder;
	/** 位置情報 */
	private Location location;
	/** 位置情報更新時プログレス */
	private ProgressBar locationProgress;
	/** 位置情報読み込み時メッセージ */
	private String msgLoadLocation;
	/** 位置情報取得失敗時メッセージ */
	private String msgLoadLocationFailed;
	/** 位置情報無効時メッセージ */
	private String locationDisabled;
	/** 現在地読み込みタイムアウト判定タイマー */
	private Timer locationTimer;
	/** ロケーション設定フラグ */
	private boolean isLocationConfigured;
	/** 最大入力数を超えた際に出力するトースト */
	private Toast inputLengthOverToast;
	/** Twitterオブジェクト */
	private Twitter twitter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tweet_main);

		this.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

		// Twitterオブジェクト取得
		twitter = ((TwitterClientBean) getApplication()).getTwitter();

		// 送信ボタン
		sendTweetButton = (Button) findViewById(R.id.btn_send_tweet);
		sendTweetButton.setOnClickListener(this);
		((Button) findViewById(R.id.btn_tweet_cancel)).setOnClickListener(this);

		// 最大入力数釣果メッセージ
		inputLengthOverToast = createToast(getString(R.string.length_over));
		// 現在地特定時メッセージ
		msgLoadLocation = getString(R.string.location_loading);
		// 現在地特定失敗時メッセージ
		msgLoadLocationFailed = getString(R.string.location_load_failed);
		// 位置情報無効時メッセージ
		locationDisabled = getString(R.string.location_disabled);
		// 位置情報更新時プログレス
		locationProgress = (ProgressBar) findViewById(R.id.location_progress);

		// 入力フィールド初期化
		initEditText();

		// 現在地
		locationView = (TextView) findViewById(R.id.location);
		// 位置情報無効
		locationView.setText(locationDisabled);
		locationView.setEnabled(false);
		locationView.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				if (locationProgress.getVisibility() == View.GONE
						&& locationView.isEnabled()) {
					showDialog(DIALOG_UPDATE_LOCATION);
				}
				return true;
			}
		});
		// 位置情報排除チェック
		locationOffCheck = (CheckBox) findViewById(R.id.location_off_check);
		locationOffCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					locationView.setEnabled(true);
					if (!isLocationConfigured) {
						String provider = locationManager.getBestProvider(new Criteria(), true);
						location = locationManager.getLastKnownLocation(provider);
						setLocation(location);
					}
				} else {
					// 位置情報無効
					locationView.setEnabled(false);
				}
			}
		});

		// 現在値設定
		geocoder = new Geocoder(this, Locale.JAPAN);
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
	}

	private void initEditText() {
		input = (EditText) findViewById(R.id.input_tweet);
		// 入力チェック
		input.addTextChangedListener(new TextWatcher() {
			/** 最大文字数超過判定 */
			private boolean isLengthOver = false;
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				int len = s.length();
				if (isLengthOver) {
					// 既に最大文字数を超えている場合
					if (len <= MAX_LENGTH) {
						isLengthOver = false;
						input.setTextColor(Color.BLACK);
						sendTweetButton.setEnabled(true);
					} else {
						return;
					}
				}
				if (MAX_LENGTH < len) {
					isLengthOver = true;
					inputLengthOverToast.show();
					input.setTextColor(Color.RED);
					sendTweetButton.setEnabled(false);
					return;
				}
				if (0 < len) {
					sendTweetButton.setEnabled(true);
				} else {
					sendTweetButton.setEnabled(false);
				}
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override
			public void afterTextChanged(Editable s) {}
		});

		Bundle bundle = getIntent().getExtras();
		if (bundle != null) {
			sendType = bundle.getInt(PARAM_SEND_TYPE);
		} else {
			sendType = 0;
		}
		// 前回入力内容保存プリファレンス
		SharedPreferences sp = getPreferences(MODE_PRIVATE);
		switch (sendType) {
		case TYPE_REPLY:
			// 返信の場合
			String replyTag = getString(R.string.reply,
					bundle.getString(PARAM_REPLY_NAME) + " ");
			preferenceKey = REPLY_INPUT + replyTag;
			String preReplyInput = sp.getString(preferenceKey, null);
			// 前回入力内容がある場合は設定
			if (preReplyInput != null && !preReplyInput.trim().isEmpty()) {
				input.setText(preReplyInput);
				input.setSelection(preReplyInput.length());
			} else {
				input.setText(replyTag);
				input.setSelection(replyTag.length());
			}
			break;
		case TYPE_HASHTAG:
			// ハッシュタグの場合
			String hashtag = bundle.getString(PARAM_TWEET_HASHTAG);
			preferenceKey = HASHTAG_INPUT + hashtag;
			String preHashtagInput = sp.getString(preferenceKey, null);
			// 前回入力内容がある場合は設定
			if (preHashtagInput != null && !preHashtagInput.trim().isEmpty()) {
				input.setText(preHashtagInput);
			} else {
				input.setText(" " + hashtag);
			}
			break;
		default:
			preferenceKey = PREVIOUSE_INPUT;
			String preInput = sp.getString(preferenceKey, null);
			// 前回入力内容がある場合は設定
			if (preInput != null && !preInput.trim().isEmpty()) {
				input.setText(preInput);
				input.setSelection(preInput.length());
			} else {
				sendTweetButton.setEnabled(false);
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (!isCompleted) {
			// 現在の状態を保存
			saveState();
		}
		// 位置情報更新中の場合は終了
		stopUpdateLocation();
	}

	/**
	 * 現在の状態を保存します。
	 */
	private void saveState() {
		SharedPreferences sp = getPreferences(MODE_PRIVATE);
		sp.edit().putString(preferenceKey, input.getText().toString()).commit();
		if (locationOffCheck.isEnabled()) {
			
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_send_tweet:
			updateStatus();
			break;
		case R.id.btn_tweet_cancel:
			// キャンセルボタン
			setResult(Activity.RESULT_CANCELED);
			finish();
			break;
		}
	}

	/**
	 * つぶやきを投稿
	 */
	private void updateStatus() {
		StatusUpdate status = new StatusUpdate(input.getText().toString());
		if (locationOffCheck.isEnabled() && location != null) {
			status.setLocation(new GeoLocation(
					location.getLatitude(), location.getLongitude()));
		}
		new UpdateStatusTask().execute(status);
	}

	/**
	 * 現在地設定
	 * @param location
	 */
	private void setLocation(Location location) {
		this.location = location;
		new GeoCodeTask().execute(location);
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
			showDialog(DIALOG_PROVIDER_NOTFOUND);
			return;
		}
		// ロケーション取得のタイムアウトを検知するタイマー
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
							// 位置情報取得失敗メッセージ
							locationView.setText(msgLoadLocationFailed);
							// 位置情報排除チェック表示
							locationOffCheck.setVisibility(View.VISIBLE);
							// プログレス非表示
							locationProgress.setVisibility(View.GONE);
						}
						time += 1000L;
					}
				});
			}
		}, 0L, 1000L);
		locationListener = new LocationListener() {
			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {}
			@Override
			public void onProviderEnabled(String provider) {}
			@Override
			public void onProviderDisabled(String provider) {}
			@Override
			public void onLocationChanged(Location location) {
				setLocation(location);
				stopUpdateLocation();
			}
		};
		// 現在地更新
		locationManager.requestLocationUpdates(provider, MIN_TIME, MIN_DISTANCE, locationListener);
	}

	private void stopUpdateLocation() {
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

	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		switch (id) {
		case DIALOG_CONNECTED_FAILED:
			return new AlertDialog.Builder(this)
			.setTitle(getString(R.string.dialog_connected_failed_title))
			.setMessage(getString(R.string.dialog_connected_failed_message))
			.setPositiveButton(getString(R.string.dialog_connected_failed_positive),
					new DialogInterface.OnClickListener() {
				@Override
				public void onClick(final DialogInterface dialog, final int which) {
					// リトライ
					updateStatus();
				}
			})
			.setNegativeButton(getString(R.string.dialog_connected_failed_negative),
					new DialogInterface.OnClickListener() {
				@Override
				public void onClick(final DialogInterface dialog, final int which) {}
			})
			.create();
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
						changeLocationViewState();
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
		case DIALOG_UPDATE_LOCATION:
			return new AlertDialog.Builder(this)
			.setMessage(getString(R.string.dialog_update_location_message))
			.setPositiveButton(getString(R.string.dialog_update_location_positive),
					new DialogInterface.OnClickListener() {
				@Override
				public void onClick(final DialogInterface dialog, final int which) {
					changeLocationViewState();
					updateLocation();
				}
			})
			.setNegativeButton(getString(R.string.dialog_update_location_negative),
					new DialogInterface.OnClickListener() {
				@Override public void onClick(final DialogInterface dialog, final int which) {}
			})
			.create();
		default:
			return null;
		}
	}

	private void changeLocationViewState() {
		// 現在地取得中メッセージ
		locationView.setText(msgLoadLocation);
		// 送信ボタン無効化
		sendTweetButton.setEnabled(false);
		// 位置情報排除チェック非表示
		locationOffCheck.setVisibility(View.GONE);
		// プログレス表示
		locationProgress.setVisibility(View.VISIBLE);
	}
	/**
	 * Toastにメッセージを設定して返します。
	 * @param text メッセージ
	 * @return Toast
	 */
	private Toast createToast(final CharSequence text) {
		Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER, 0, 0);
		return toast;
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
		protected void onPreExecute() {
			if (sendTweetButton.isEnabled()) {
				sendTweetButton.setEnabled(false);
			}
			locationProgress.setVisibility(View.VISIBLE);
			locationOffCheck.setVisibility(View.GONE);
			locationView.setText(msgLoadLocation);
		}
		@Override
		protected void onPostExecute(String result) {
			if (result != null) {
				isLocationConfigured = true;
				locationView.setText(result);
			} else {
				// ジオコード失敗
				locationView.setText(msgLoadLocationFailed);
			}
			if (0 < input.getText().length()) {
				sendTweetButton.setEnabled(true);
			}
			locationProgress.setVisibility(View.GONE);
			locationOffCheck.setVisibility(View.VISIBLE);
		}
	}

	private class UpdateStatusTask extends AsyncTask<StatusUpdate, Integer, Boolean> {
		private ProgressDialog progress;
		@Override
		protected Boolean doInBackground(StatusUpdate... status) {
			try {
				twitter.updateStatus(status[0]);
				return true;
			} catch (TwitterException e) {
			}
			return false;
		}
		@Override
		protected void onPreExecute() {
			progress = new ProgressDialog(TweetMain.this);
			progress.setMessage(getString(R.string.progress_sending));
			progress.show();
		}
		@Override
		protected void onPostExecute(Boolean result) {
			progress.dismiss();
			isCompleted = result;
			if (result) {
				SharedPreferences sp = getPreferences(MODE_PRIVATE);
				sp.edit().remove(preferenceKey).commit();
				setResult(Activity.RESULT_OK);
				finish();
			} else {
				showDialog(DIALOG_CONNECTED_FAILED);
			}
		}
	}
}
