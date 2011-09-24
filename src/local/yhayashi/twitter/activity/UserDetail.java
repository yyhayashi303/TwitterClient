package local.yhayashi.twitter.activity;

import static local.yhayashi.twitter.activity.TweetMain.PARAM_REPLY_NAME;
import static local.yhayashi.twitter.activity.TweetMain.PARAM_SEND_TYPE;
import static local.yhayashi.twitter.activity.TweetMain.TYPE_REPLY;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import local.yhayashi.twitter.common.ImageCache;
import local.yhayashi.twitter.common.TwitterListAdapter;
import local.yhayashi.twitter.db.TweetsInfo;
import local.yhayashi.twitter.db.TweetsInfoFactory;
import local.yhayashi.twitter.task.ImagePreLoadTask2;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class UserDetail extends Activity {
	public static final String PARAM_USER_ID = "user_id";
	public static final String PARAM_USER_NAME = "user_name";
	/** コンテキストメニュー：返信 */
	private static final int CONTEXT_MENU_REPLY = Menu.FIRST + 1;
	/** コンテキストメニュー：共有 */
	private static final int CONTEXT_MENU_SHARE = Menu.FIRST + 2;

	/** ユーザー名 */
	private String userName;
	/** ListView */
	private ListView listView;
	/** ツイッターアダプタ */
	private TwitterListAdapter adapter;
	/** フッター */
	private View footer;
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
	/** タイムライン取得非同期タスク */
	private LoadHomeTimeLineTask task;
	/** Twitterオブジェクト */
	private Twitter twitter;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.user_detail);

		twitter = ((TwitterClientBean) getApplication()).getTwitter();

		Bundle bundle = getIntent().getExtras();
		userName = bundle.getString(PARAM_USER_NAME);

		try {
			User user = twitter.showUser(userName);
			// アイコン画像
			((ImageView) findViewById(R.id.item_detail_icon)).setImageBitmap(
					ImageCache.getImage(user.getProfileImageURL().toString()));
			String screenName = user.getScreenName();
			// スクリーン名
			((TextView) findViewById(R.id.item_detail_screen_name)).setText(
					getString(R.string.detail_screan_name, screenName));
			String name = user.getName();
			if (name == null) {
				name = screenName;
			}
			// ユーザ名
			((TextView) findViewById(R.id.item_detail_user_name)).setText(name);
			// 自己紹介
			((TextView) findViewById(R.id.user_description)).setText(user.getDescription());

			// ListView初期化
			initListView();
			new LoadHomeTimeLineTask(LoadHomeTimeLineTask.INSERT).execute(new Paging());
		} catch (TwitterException e) {
			Log.e("UserDetail", "", e);
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
//				if (refreshButton.isEnabled() == true && visibleItemCount < totalItemCount) {
				if (visibleItemCount < totalItemCount) {
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
				Intent intent = new Intent(UserDetail.this, TweetDetail.class);
				intent.putExtra(TweetDetail.PARAM_TWEET_INFO, adapter.getItem(position));
				startActivity(intent);
			}
		});
		// コンテキストメニュー登録
		registerForContextMenu(listView);
	}

	/**
	 * statusId以降のツイートを取得して表示します。
	 * @param statusId
	 */
	private void fetchMore(long statusId) {
//		refreshButton.setEnabled(false);
		Paging paging = new Paging();
		paging.setMaxId(statusId);
		paging.setCount(50);
		task = new LoadHomeTimeLineTask(LoadHomeTimeLineTask.ADD);
		task.execute(paging);
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
				ResponseList<twitter4j.Status> responseList = twitter.getUserTimeline(userName, pagings[0]);
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
//			refreshButton.setEnabled(true);
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
//						footerProgressStateChange(false);
					}
					adapter.notifyDataSetChanged();
					break;
				}
			} else {
				footerMessageView.setText(loadFailure);
//				footerProgressStateChange(false);
			}
		}

		@Override
		protected void onCancelled() {
			footer.setVisibility(View.GONE);
//			refreshButton.setEnabled(true);
			setProgress(false);
		}
		private void progressOn() {
			if (adapter.getCount() == 0) {
				isInitialize = true;
				progress = new ProgressDialog(UserDetail.this);
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
