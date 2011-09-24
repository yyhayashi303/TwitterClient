package local.yhayashi.twitter.activity;

import static local.yhayashi.twitter.activity.TweetMain.PARAM_REPLY_NAME;
import static local.yhayashi.twitter.activity.TweetMain.PARAM_SEND_TYPE;
import static local.yhayashi.twitter.activity.TweetMain.TYPE_REPLY;
import static local.yhayashi.twitter.activity.TwitterOAuth.HTTP_PARAM_OAUTH_VERIFIER;
import static local.yhayashi.twitter.activity.TwitterOAuth.PARAM_OAUTH_URL;
import static local.yhayashi.twitter.map.TweetsMap.PARAM_TWEETS_INFO;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import local.yhayashi.twitter.common.HashtagListAdapter;
import local.yhayashi.twitter.common.ImageCache;
import local.yhayashi.twitter.common.TwitterListAdapter;
import local.yhayashi.twitter.db.HashtagListData;
import local.yhayashi.twitter.db.HashtagListDataDao;
import local.yhayashi.twitter.db.HashtagListDataDaoFactory;
import local.yhayashi.twitter.db.TimelineCacheDao;
import local.yhayashi.twitter.db.TimelineCacheDaoFactory;
import local.yhayashi.twitter.db.TweetsInfo;
import local.yhayashi.twitter.db.TweetsInfoFactory;
import local.yhayashi.twitter.map.TweetsMap;
import local.yhayashi.twitter.task.ImagePreLoadTask2;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class TwitterClientHome extends Activity implements View.OnClickListener {
	/** コンシューマキー */
	private static final String CONSUMER_KEY="jtG5TfJNrVr7B2G0zi8MAw";
	/** コンシューマシークレット */
	private static final String CONSUMER_SECRET = "QEr7FR8c48OHANeIIZZSvbxY9bjEraHe4dyTACYoE";
	/** OAuth認証アクティビティ起動リクエストコード */
	private static final int REQUEST_OAUTH = 0;
	/** ツイートアクティビティ起動リクエストコード */
	private static final int REQUEST_TWEET = 1;
	/** Preferenceキー：アクセストークン */
	private static final String PRE_KEY_ACCESS_TOKEN = "key_access_token";
	/** Preferenceキー：アクセストークンシークレット */
	private static final String PRE_KEY_ACCESS_TOKEN_SECRET = "key_access_token_secret";
	/** コンテキストメニュー：返信 */
	private static final int CONTEXT_MENU_REPLY = Menu.FIRST + 1;
	/** コンテキストメニュー：共有 */
	private static final int CONTEXT_MENU_SHARE = Menu.FIRST + 2;
	/** オプションメニュー：ハッシュタグ一覧 */
	private static final int OPTION_MENU_HASHTAG_LIST = Menu.FIRST + 1;
	/** オプションメニュー：地図表示 */
	private static final int OPTION_MENU_SHOW_MAP = Menu.FIRST + 2;
	/** ダイアログ OAuth認証失敗 */
	private static final int DIALOG_OAUTH_FAILED = 0;
	/** ダイアログ サービス未接続 */
	private static final int DIALOG_CONNECTED_FAILED = 1;
	/** ダイアログ ハッシュタグ一覧削除確認 */
	private static final int DIALOG_HASHTAG_DELETE_CONFIRM = 3;
	/** タイムライン保存時のリスト名 */
	private static final String LIST_HOME_TIMELINE = "home_timeline";

	/** アプリ内データ共有Bean */
	private TwitterClientBean bean;
	/** ツイートリスト */
	private ListView listView;
	/** ツイッターアダプタ */
	private TwitterListAdapter adapter;
	/** フッター */
	private View footer;
	/** タイムライン保存DAO */
	private TimelineCacheDao cacheDao;
	/** ハッシュタグ一覧保存DAO */
	private HashtagListDataDao hashtagDao;
	/** DBキャッシュクリア判定 */
	private boolean clearCache;
	/** 更新ボタン */
	private Button refreshButton;
	/** リクエストトークン */
	private RequestToken requestToken;
	/** タイムライン取得非同期タスク */
	private LoadHomeTimeLineTask task;
	/** フッター 読み込み中プログレス */
	private ProgressBar footerProgress;
	/** フッター 読み込み中メッセージ表示View */
	private TextView footerLoadingView;
	/** フッターメッセージ表示View */
	private TextView footerMessageView;
	/** フッター読み込み失敗メッセージ */
	private String loadFailure;
	/** フッター読み込み上限メッセージ */
	private String loadOver;
	/** プリファレンス */
	private SharedPreferences sp;
	/** ハッシュタグ一覧のListView */
	private ListView hashtagListView;
	/** ハッシュタグ一覧のアダプタ */
	private HashtagListAdapter hashtagListAdapter;
	/** ハッシュタグ一覧表示ダイアログ */
	private AlertDialog hashtagListDialog;
	/** 削除対象ハッシュタグ */
	private String selectedHashtag;
	/** ツイッターオブジェクト */
	private Twitter twitter;

	private static final boolean TEST_MODE = true;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.list_main);

		// つぶやくボタン
		((Button) findViewById(R.id.btn_tweet)).setOnClickListener(this);
		// 更新ボタン
		((Button) findViewById(R.id.btn_nearby)).setOnClickListener(this);
		refreshButton = (Button) findViewById(R.id.btn_refresh);
		refreshButton.setOnClickListener(this);

		// ListView初期化
		initListView();

		// プリファレンス取得
		sp = getPreferences(MODE_PRIVATE);

		// Bean生成
		bean = (TwitterClientBean) getApplication();
		// Twitterオブジェクト生成
		twitter = new TwitterFactory().getInstance();
		twitter.setOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
		// アプリ内で共有
		bean.setTwitter(twitter);

		// DAO生成
		cacheDao = TimelineCacheDaoFactory.getFactory().getTimelineCacheDao(this);
		hashtagDao = HashtagListDataDaoFactory.getFactory().getHashtagListDao(this);

		// ハッシュタグ一覧生成
		createHashtagList();

		// アクセストークン取得
		String token = sp.getString(PRE_KEY_ACCESS_TOKEN, null);
		String tokenSecret = sp.getString(PRE_KEY_ACCESS_TOKEN_SECRET, null);
		// アクセストークンがある場合
		if (token != null && tokenSecret != null) {
			twitter.setOAuthAccessToken(new AccessToken(token, tokenSecret));
			List<TweetsInfo> dtoList = cacheDao.findList(LIST_HOME_TIMELINE, null);
			// DBキャッシュがある場合はアダプターに設定
			if (0 < dtoList.size()) {
				for (TweetsInfo dto : dtoList) {
					adapter.add(dto);
				}
			}
			// ユーザ情報生成
			createUserInfo();
			// タイムライン取得
			updateTimeline();
		} else {
			// アクセストークンがない場合はOAuth認証開始
			oAuth();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		// 終了していないタスクをキャンセル
		if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
			task.cancel(true);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (clearCache || listView == null) {
			return;
		}
		List<TweetsInfo> dtoList = new ArrayList<TweetsInfo>();
		int count = adapter.getCount();
		count = count > 20 ? 20:count;
		for (int i = 0; i < count; i++) {
			TweetsInfo dto = adapter.getItem(i);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			Bitmap icon = ImageCache.getImage(dto.getProfileImageURL());
			if (icon != null && icon.compress(Bitmap.CompressFormat.PNG, 100,baos)) {
				dto.setBitmap(baos.toByteArray());
			}
			dto.setListName(LIST_HOME_TIMELINE);
			dtoList.add(dto);
		}
		cacheDao.deleteList(LIST_HOME_TIMELINE);
		cacheDao.insert(dtoList);
		if (cacheDao != null) {
			cacheDao.close();
		}
	}

	/**
	 * 起動したアクティビティの実行結果を受け取る
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		switch (requestCode) {
		case REQUEST_OAUTH:
			if (intent == null) {
				showDialog(DIALOG_OAUTH_FAILED);
				return;
			}
			String verifier = intent.getExtras().getString(HTTP_PARAM_OAUTH_VERIFIER);
			if (verifier == null) {
				showDialog(DIALOG_OAUTH_FAILED);
				return;
			}
			AccessToken accessToken = null;
			try {
				accessToken = twitter.getOAuthAccessToken(requestToken, verifier);
				// アクセストークン保存
				SharedPreferences.Editor editor = sp.edit();
				editor.putString(PRE_KEY_ACCESS_TOKEN, accessToken.getToken());
				editor.putString(PRE_KEY_ACCESS_TOKEN_SECRET, accessToken.getTokenSecret());
				editor.commit();
				// タイムライン取得
				updateTimeline();
			} catch (TwitterException e) {
				showDialog(DIALOG_OAUTH_FAILED);
			}
			break;
		case REQUEST_TWEET:
			if (resultCode == RESULT_OK) {
				updateTimeline();
			}
			break;
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_tweet:
			Intent tweetIntent = new Intent(this, TweetMain.class);
			startActivityForResult(tweetIntent, REQUEST_TWEET);
			break;
		case R.id.btn_nearby:
			Intent nearbyIntent = new Intent(this, Nearby.class);
			startActivity(nearbyIntent);
			break;
		case R.id.btn_refresh:
			updateTimeline();
			break;
		}
	}
	private void oAuth() {
		try {
			requestToken = twitter.getOAuthRequestToken(TwitterOAuth.CALLBACK_URL);
			Intent intent = new Intent(this, TwitterOAuth.class);
			intent.putExtra(PARAM_OAUTH_URL, requestToken.getAuthorizationURL());
			startActivityForResult(intent, REQUEST_OAUTH);
		} catch (TwitterException e) {
			showDialog(DIALOG_OAUTH_FAILED);
		}
	}

	/**
	 * ListView初期化
	 */
	private void initListView() {
		listView = (ListView) findViewById(R.id.list_view);
		// フッター
		footer = getLayoutInflater().inflate(R.layout.list_footer, null);
		footer.setVisibility(View.GONE);
		// フッタープログレス
		footerProgress = (ProgressBar) footer.findViewById(R.id.footer_now_loading_progress);
		// フッター読み込み中メッセージView
		footerLoadingView = (TextView) footer.findViewById(R.id.footer_now_loading);
		// フッター読み込み失敗メッセージView
		footerMessageView = (TextView) footer.findViewById(R.id.footer_message);
		// フッター表示メッセージ　読み込み失敗 
		loadFailure = getString(R.string.footer_load_failure);
		// フッター表示メッセージ　これ以上読み込めない場合
		loadOver = getString(R.string.footer_load_over);
		// フッター追加
		listView.addFooterView(footer);
		// アダプター追加
		adapter = new TwitterListAdapter(this, new ArrayList<TweetsInfo>());
		listView.setAdapter(adapter);
		// 高速スクロール
		listView.setFastScrollEnabled(true);
		listView.setOnScrollListener(new AbsListView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {}
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				// 更新ボタンとの排他
				if (refreshButton.isEnabled() == true && visibleItemCount < totalItemCount) {
					// 最終行かつ、読み込みエラーが発生していない場合は読み込む
					if (footerMessageView.getVisibility() == View.GONE
							&& totalItemCount == (firstVisibleItem + visibleItemCount)) {
						footer.setVisibility(View.VISIBLE);
						// 現在取得しているもの以降を取得
						TweetsInfo tweets = adapter.getItem(adapter.getCount() -1);
						fetchMore(tweets.getStatusId());
					}
				}
				// 読み込みエラーが発生して、その後フッターが見えなくなったら状態を戻す
				if (footerMessageView.getVisibility() == View.VISIBLE
						&& totalItemCount -1 == (firstVisibleItem + visibleItemCount)) {
					footerProgressStateChange(true);
				}
			}
		});
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (footer.equals(view)) {
					return;
				}
				// 選択されたアイテムの詳細ページへ遷移
				Intent intent = new Intent(TwitterClientHome.this, TweetDetail.class);
				intent.putExtra(TweetDetail.PARAM_TWEET_INFO, adapter.getItem(position));
				startActivity(intent);
			}
		});
		// コンテキストメニュー登録
		registerForContextMenu(listView);
	}

	private void createUserInfo() {
//		try {
//			new ArrayList<Long>();
//			long[] aa = twitter.getFriendsIDs(-1L).getIDs();
//			Arrays.asList(aa);
//			twitter.getFriendsIDs(-1L);
//			twitter.getFollowersIDs(-1L);
//		} catch (TwitterException e) {
//			e.printStackTrace();
//		}
	}
	private void createHashtagList() {
		// ハッシュタグ一覧のアダプタ生成
		hashtagListAdapter = new HashtagListAdapter(this, new ArrayList<String>());
		List<HashtagListData> hashtagList = hashtagDao.findAll(null);
		for (HashtagListData dto : hashtagList) {
			hashtagListAdapter.add(dto.getName());
		}
		bean.setHashtagListAdapter(hashtagListAdapter);

		// ハッシュタグListView生成
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		hashtagListView = (ListView) inflater.inflate(R.layout.hashtag_list_view, null);
		hashtagListView.setAdapter(hashtagListAdapter);
		hashtagListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Intent intent = new Intent(TwitterClientHome.this, HashTagMain.class);
				intent.putExtra(HashTagMain.PARAM_HASHTAG,
						hashtagListAdapter.getItem(position));
				startActivity(intent);
				hashtagListDialog.dismiss();
			}
		});
		hashtagListView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				selectedHashtag = hashtagListAdapter.getItem(position);
				showDialog(DIALOG_HASHTAG_DELETE_CONFIRM);
				return true;
			}
		});
		// ハッシュタグ一覧ダイアログ生成
		hashtagListDialog = new AlertDialog.Builder(this)
		.setTitle(getString(R.string.dialog_hashtag_title))
		.setView(hashtagListView)
		.create();
	}
	/**
	 * 最新のタイムラインを取得して表示します。
	 */
	private void updateTimeline() {
		refreshButton.setEnabled(false);
		Paging paging;
		if (0 < adapter.getCount()) {
			TweetsInfo tweets = adapter.getItem(0);
			// 現在取得している最新以降を取得
			paging = new Paging(tweets.getStatusId());
		} else {
			paging = new Paging();
		}
		paging.setCount(50);
		task = new LoadHomeTimeLineTask(LoadHomeTimeLineTask.INSERT);
		task.execute(paging);
	}

	/**
	 * statusId以降のツイートを取得して表示します。
	 * @param statusId
	 */
	private void fetchMore(long statusId) {
		refreshButton.setEnabled(false);
		Paging paging = new Paging();
		paging.setMaxId(statusId);
		paging.setCount(50);
		task = new LoadHomeTimeLineTask(LoadHomeTimeLineTask.ADD);
		task.execute(paging);
	}

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
		startActivity(intent);
	}


	/**
	 * フターのプログレスの状態を変更します。
	 * @param progressOn trueの場合、プログレスを表示
	 */
	private void footerProgressStateChange(boolean progressOn) {
		if (progressOn) {
			footer.setVisibility(View.GONE);
			footerMessageView.setVisibility(View.GONE);
			footerProgress.setVisibility(View.VISIBLE);
			footerLoadingView.setVisibility(View.VISIBLE);
		} else {
			footer.setVisibility(View.VISIBLE);
			footerMessageView.setVisibility(View.VISIBLE);
			footerProgress.setVisibility(View.GONE);
			footerLoadingView.setVisibility(View.GONE);
		}
	}

	private void deleteHashtag() {
		hashtagListAdapter.remove(selectedHashtag);
		hashtagListAdapter.notifyDataSetChanged();
		hashtagDao.delete(selectedHashtag);
	}

	/**
	 * オプションメニュー
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, OPTION_MENU_HASHTAG_LIST, Menu.NONE,
				getString(R.string.option_menu_hashtag_list));
		menu.add(Menu.NONE, OPTION_MENU_SHOW_MAP, Menu.NONE,
				getString(R.string.option_menu_show_map));
		if (TEST_MODE) {
			menu.add(Menu.NONE, Menu.FIRST+3, Menu.NONE, "キャッシュ削除");
			menu.add(Menu.NONE, Menu.FIRST+4, Menu.NONE, "トークン削除");
		}
		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * オプションメニューが選択された際の処理
	 * @param item
	 * @return
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case OPTION_MENU_HASHTAG_LIST:
			hashtagListDialog.show();
			return true;
		case OPTION_MENU_SHOW_MAP:
			showMap();
			return true;
		case Menu.FIRST + 3:
			cacheDao.deleteList(LIST_HOME_TIMELINE);
			clearCache = true;
			return true;
		case Menu.FIRST + 4:
			sp.edit().clear().commit();
		}
		return super.onOptionsItemSelected(item);
	}
	/**
	 * コンテキストメニュー
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (footer.equals(((AdapterContextMenuInfo) menuInfo).targetView)) {
			return;
		}
		menu.setHeaderTitle(getString(R.string.context_menu_title));
		menu.add(Menu.NONE, CONTEXT_MENU_REPLY, Menu.NONE, getString(R.string.context_menu_reply));
		menu.add(Menu.NONE, CONTEXT_MENU_SHARE, Menu.NONE, getString(R.string.context_menu_share));
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
		case CONTEXT_MENU_REPLY:
			Intent replyIntent = new Intent(this, TweetMain.class);
			replyIntent.putExtra(PARAM_SEND_TYPE, TYPE_REPLY);
			replyIntent.putExtra(PARAM_REPLY_NAME, tweets.getScreenName());
			startActivity(replyIntent);
			return true;
		case CONTEXT_MENU_SHARE:
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

	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		switch (id) {
		case DIALOG_OAUTH_FAILED:
			return new AlertDialog.Builder(this)
			.setTitle(getString(R.string.dialog_oauth_failed_title))
			.setMessage(getString(R.string.dialog_oauth_failed_message))
			.setPositiveButton(getString(R.string.dialog_oauth_failed_positive),
					new DialogInterface.OnClickListener() {
				@Override
				public void onClick(final DialogInterface dialog, final int which) {
					// 再度認証
					twitter = new TwitterFactory().getInstance();
					twitter.setOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
					oAuth();
				}
			})
			.setNegativeButton(getString(R.string.dialog_oauth_failed_negative),
					new DialogInterface.OnClickListener() {
				@Override
				public void onClick(final DialogInterface dialog, final int which) {
					finish();
				}
			})
			.create();
		case DIALOG_CONNECTED_FAILED:
			return new AlertDialog.Builder(this)
			.setTitle(getString(R.string.dialog_connected_failed_title))
			.setMessage(getString(R.string.dialog_connected_failed_message))
			.setPositiveButton(getString(R.string.dialog_connected_failed_positive),
					new DialogInterface.OnClickListener() {
				@Override
				public void onClick(final DialogInterface dialog, final int which) {
					// リトライ
					updateTimeline();
				}
			})
			.setNegativeButton(getString(R.string.dialog_connected_failed_negative),
					new DialogInterface.OnClickListener() {
				@Override
				public void onClick(final DialogInterface dialog, final int which) {}
			})
			.create();
		case DIALOG_HASHTAG_DELETE_CONFIRM:
			return new AlertDialog.Builder(this)
			.setMessage(getString(R.string.dialog_hashtag_delete_confirm))
			.setPositiveButton(getString(R.string.dialog_hashtag_delete_yes),
					new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// ハッシュタグ一覧から削除
					deleteHashtag();
				}
			})
			.setNegativeButton(getString(R.string.dialog_hashtag_delete_no),
					new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {}
			})
			.create();
		default:
			return null;
		}
	}

	/**
	 * タイムライン取得非同期タスク
	 */
	private class LoadHomeTimeLineTask extends AsyncTask<Paging, Integer, List<TweetsInfo>> {
		/** 更新タイプ 先頭に追加 */
		private static final int INSERT = 0;
		/** 更新タイプ 末尾に追加 */
		private static final int ADD = 1;

		/** 更新タイプ */
		private final int updateType;
		/** 初期化フラグ */
		private boolean isInitialize;
		/** プログレス */
		private ProgressDialog progress;

		public LoadHomeTimeLineTask(int updateType) {
			this.updateType = updateType;
		}

		@Override
		protected List<TweetsInfo> doInBackground(Paging... pagings) {
			try {
				List<URL> urlList = new ArrayList<URL>();
				List<TweetsInfo> list = new ArrayList<TweetsInfo>();
				ResponseList<twitter4j.Status> responseList = twitter.getHomeTimeline(pagings[0]);
				for (twitter4j.Status status : responseList) {
					// キャンセルされた場合は終了
					if (isCancelled()) {
						return null;
					}
					list.add(TweetsInfoFactory.createTweetsInfo(status));
					URL url = status.getUser().getProfileImageURL();
					if (ImageCache.getImage(url.toExternalForm()) == null) {
						urlList.add(url);
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
			progressOn();
		}
		@Override
		protected void onPostExecute(List<TweetsInfo> list) {
			refreshButton.setEnabled(true);
			progressOff();
			if (list != null) {
				int size = list.size();
				footer.setVisibility(View.GONE);
				switch (updateType) {
				case INSERT:
					for (int i = size -1; 0 <= i; i--) {
						adapter.insert(list.get(i), 0);
					}
					break;
				case ADD:
					for (int i = 1; i < size; i++) {
						adapter.add(list.get(i));
					}
					// 取得結果0の場合はフッターにメッセージを表示
					if (size == 1) {
						footerMessageView.setText(loadOver);
						footerProgressStateChange(false);
					}
					adapter.notifyDataSetChanged();
					break;
				}
			} else {
				footerMessageView.setText(loadFailure);
				footerProgressStateChange(false);
			}
		}

		@Override
		protected void onCancelled() {
			footer.setVisibility(View.GONE);
			refreshButton.setEnabled(true);
			setProgress(false);
		}
		private void progressOn() {
			if (adapter.getCount() == 0) {
				isInitialize = true;
				progress = new ProgressDialog(TwitterClientHome.this);
				progress.setMessage(getString(R.string.load_tweet));
				progress.show();
			} else {
				setProgress(true);
			}
		}
		private void progressOff() {
			if (isInitialize) {
				progress.dismiss();
			} else {
				setProgress(false);
			}
		}

		private void setProgress(boolean enable) {
			setProgressBarIndeterminate(enable);
			setProgressBarIndeterminateVisibility(enable);
		}
	}
}