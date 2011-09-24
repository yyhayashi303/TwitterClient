package local.yhayashi.twitter.activity;

import static local.yhayashi.twitter.activity.TweetMain.PARAM_REPLY_NAME;
import static local.yhayashi.twitter.activity.TweetMain.PARAM_SEND_TYPE;
import static local.yhayashi.twitter.activity.TweetMain.PARAM_TWEET_HASHTAG;
import static local.yhayashi.twitter.activity.TweetMain.TYPE_HASHTAG;
import static local.yhayashi.twitter.activity.TweetMain.TYPE_REPLY;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import local.yhayashi.twitter.common.HashtagListAdapter;
import local.yhayashi.twitter.common.ImageCache;
import local.yhayashi.twitter.common.TwitterListAdapter;
import local.yhayashi.twitter.db.HashtagListData;
import local.yhayashi.twitter.db.HashtagListDataDao;
import local.yhayashi.twitter.db.HashtagListDataDaoFactory;
import local.yhayashi.twitter.db.TweetsInfo;
import local.yhayashi.twitter.db.TweetsInfoFactory;
import local.yhayashi.twitter.task.ImagePreLoadTask2;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Tweet;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class HashTagMain extends Activity implements View.OnClickListener {
	/** インテントパラメータ：ハッシュタグ */
	public static final String PARAM_HASHTAG = "key_hashtag";

	/** ツイートアクティビティ起動リクエストコード */
	private static final int REQUEST_TWEET = 1;
	/** コンテキストメニュー：返信 */
	private static final int CONTEXT_MENU_REPLY = Menu.FIRST + 1;
	/** コンテキストメニュー：共有 */
	private static final int CONTEXT_MENU_SHARE = Menu.FIRST + 2;
	/** オプションメニュー：ハッシュタグ一覧追加 */
	private static final int OPTION_MENU_HASHTAG_ADD = Menu.FIRST + 1;
	/** １ページの最大読み込み数 */
	private static final int MAX_COUNT_PER_PAGE = 40;

	/** ハッシュタグツイートリスト */
	private ListView listView;
	/** ツイッターアダプタ */
	private TwitterListAdapter adapter;
	/** フッター */
	private View footer;
	/** 更新ボタン */
	private Button refreshButton;
	/** ハッシュタグツイート取得タスク */
	private LoadHashTagTask task;
	/** ハッシュタグ */
	private String hashTag;
	/** ハッシュタグ一覧保存DAO */
	private HashtagListDataDao hashtagDao;
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
	/** ハッシュタグ一覧登録完了メッセージ */
	private Toast hashtagAddToast;
	/** ハッシュタグ一覧に既に登録されている場合のメッセージ */
	private Toast hashtagAlreadyAddedToast;
	/** ハッシュタグ一覧のアダプタ */
	private HashtagListAdapter hashtagListAdapter;
	/** Twitterオブジェクト */
	private Twitter twitter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// カスタムタイトル使用
//		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.hashtag_main);
//		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title);

		hashtagDao = HashtagListDataDaoFactory.getFactory().getHashtagListDao(this);

		TwitterClientBean bean = (TwitterClientBean) getApplication();
		// Twitterオブジェクト取得
		twitter = bean.getTwitter();
		// ハッシュタグ一覧のアダプタ取得
		hashtagListAdapter = bean.getHashtagListAdapter();

		// ハッシュタグ取得
		Bundle bundle = getIntent().getExtras();
		hashTag = bundle.getString(PARAM_HASHTAG);

		// トースト生成
		hashtagAddToast = Toast.makeText(
				this, getString(R.string.hashtag_add), Toast.LENGTH_SHORT);
		hashtagAlreadyAddedToast = Toast.makeText(
				this, getString(R.string.hashtag_already_added), Toast.LENGTH_SHORT);

		// 更新ボタン
		refreshButton = (Button) findViewById(R.id.btn_hashtag_refresh);
		refreshButton.setOnClickListener(this);
		// つぶやくボタン
		((Button) findViewById(R.id.btn_hashtag_tweet)).setOnClickListener(this);

		// ListView初期化
		initListView();
		// ハッシュタグ読み込み
		updateTimeline();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (hashtagDao != null) {
			hashtagDao.close();
		}
	}

	/**
	 * 起動したアクティビティの実行結果を受け取る
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_TWEET:
			if (resultCode == RESULT_OK) {
				updateTimeline();
			}
			break;
		}
	}

	/**
	 * ListView初期化
	 */
	private void initListView() {
		listView = (ListView) findViewById(R.id.hashtag_list);
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
				Intent intent = new Intent(HashTagMain.this, TweetDetail.class);
				intent.putExtra(TweetDetail.PARAM_TWEET_INFO, adapter.getItem(position));
				startActivity(intent);
			}
		});
		// コンテキストメニュー登録
		registerForContextMenu(listView);
	}


	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.btn_hashtag_tweet:
			Intent tweetIntent = new Intent(this, TweetMain.class);
			tweetIntent.putExtra(PARAM_SEND_TYPE, TYPE_HASHTAG);
			tweetIntent.putExtra(PARAM_TWEET_HASHTAG, hashTag);
			startActivityForResult(tweetIntent, REQUEST_TWEET);
			break;
		case R.id.btn_hashtag_refresh:
			updateTimeline();
			break;
		}
	}

	/**
	 * 最新のタイムラインを取得して表示します。
	 */
	private void updateTimeline() {
		Query query = new Query(hashTag);
		query.setRpp(MAX_COUNT_PER_PAGE);
		if (0 < adapter.getCount()) {
			TweetsInfo tweets = adapter.getItem(0);
			// 現在取得している最新以降を取得
			query.setSinceId(tweets.getStatusId());
		}
		task = new LoadHashTagTask(LoadHashTagTask.INSERT);
		task.execute(query);
	}

	/**
	 * statusId以降のツイートを取得して表示します。
	 * @param statusId
	 */
	private void fetchMore(long statusId) {
		Query query = new Query(hashTag);
		query.setRpp(MAX_COUNT_PER_PAGE);
		query.setMaxId(statusId);
		task = new LoadHashTagTask(LoadHashTagTask.ADD);
		task.execute(query);
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

	/**
	 * オプションメニュー
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, OPTION_MENU_HASHTAG_ADD, Menu.NONE, "ハッシュタグ一覧に追加");
		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * オプションメニューが選択された際の処理
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case OPTION_MENU_HASHTAG_ADD:
			// ハッシュタグ一覧に追加
			if (hashtagListAdapter.getPosition(hashTag) == -1) {
				HashtagListData dto = new HashtagListData();
				dto.setName(hashTag);
				hashtagDao.insert(dto);
				hashtagListAdapter.add(hashTag);
				hashtagAddToast.show();
			} else {
				hashtagAlreadyAddedToast.show();
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private class LoadHashTagTask extends AsyncTask<Query, Integer, List<TweetsInfo>> {
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

		private LoadHashTagTask(int updateType) {
			this.updateType = updateType;
		}
		@Override
		protected List<TweetsInfo> doInBackground(Query... querys) {
			List<URL> urlList = new ArrayList<URL>();
			try {
				List<TweetsInfo> list = new ArrayList<TweetsInfo>();
				QueryResult result = twitter.search(querys[0]);
				for (Tweet tweet : result.getTweets()) {
					list.add(TweetsInfoFactory.createTweetsInfo(tweet));
					String url = tweet.getProfileImageUrl();
					if (ImageCache.getImage(url) == null) {
						// アイコンがキャッシュされていない場合は読み込んでおく
						try {
							urlList.add(new URL(url));
						} catch (MalformedURLException e) {
							e.printStackTrace();
						}
					}
				}
				if (0 < urlList.size()) {
					new ImagePreLoadTask2().execute(urlList.toArray(new URL[]{}));
				}
				return list;
			} catch (TwitterException e) {
				Log.e("", "", e);
			}
			return null;
		}
		@Override
		protected void onPreExecute() {
			refreshButton.setEnabled(false);
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
		private void progressOn() {
			if (adapter.getCount() == 0) {
				isInitialize = true;
				progress = new ProgressDialog(HashTagMain.this);
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
