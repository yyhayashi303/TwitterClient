package local.yhayashi.twitter.activity;

import static local.yhayashi.twitter.activity.TweetMain.PARAM_REPLY_NAME;
import static local.yhayashi.twitter.activity.TweetMain.PARAM_SEND_TYPE;
import static local.yhayashi.twitter.activity.TweetMain.TYPE_REPLY;

import java.text.DateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import local.yhayashi.twitter.common.AddressCache;
import local.yhayashi.twitter.common.HashTagClickable;
import local.yhayashi.twitter.common.ImageCache;
import local.yhayashi.twitter.db.TweetsInfo;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class TweetDetail extends Activity implements View.OnClickListener {
	/** インテントパラメータ：ツイート情報 */
	public static final String PARAM_TWEET_INFO = "tweet_info";
	/** ダイアログ フォロー確認 */
	private static final int DIALOG_CONFIRM_FOLLOW = 0;
	/** ダイアログ フォロー解除確認 */
	private static final int DIALOG_CONFIRM_RELEASE_FOLLOW = 1;

	/** ハッシュタグ正規表現 */
	private Pattern hashtagPattern = Pattern.compile("((#|＃)[a-zA-Z0-9_\u3041-\u3094\u3099-\u309C\u30A1-\u30FA\u3400-\uD7FF\uFF10-\uFF19\uFF20-\uFF3A\uFF41-\uFF5A\uFF66-\uFF9E\u30a1-\u30fc]+)");
	/** ツイート情報 */
	private TweetsInfo tweets;
	/** Twitterオブジェクト */
	private Twitter twitter;
	/** フレンドシップ */
	private boolean isFollows;
	/** フォローボタン */
	private Button followButton;
	/** Toast:フォロー成功 */
	private Toast followSuccess;
	/** Toast:フォロー解除成功 */
	private Toast releaseFollowSucess;
	/** Toast:フォロー失敗 */
	private Toast followFailed;
	/** Toast:フォロー解除失敗 */
	private Toast releasefollowFailed;
	DateFormat dateFormat;
	DateFormat timeFormat;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tweet_detail);

		dateFormat = android.text.format.DateFormat.getDateFormat(this);
		timeFormat = android.text.format.DateFormat.getTimeFormat(this);

		twitter = ((TwitterClientBean) getApplication()).getTwitter();

		Bundle bundle = getIntent().getExtras();
		tweets = (TweetsInfo) bundle.getSerializable(PARAM_TWEET_INFO);
		Bitmap icon = ImageCache.getImage(tweets.getProfileImageURL());
		// アイコン画像
		((ImageView) findViewById(R.id.item_detail_icon)).setImageBitmap(icon);
		String screenName = tweets.getScreenName();
		// スクリーン名
		((TextView) findViewById(R.id.item_detail_screen_name)).setText(
				getString(R.string.detail_screan_name, screenName));
		String name = tweets.getName();
		if (name == null) {
			name = screenName;
		}
		// ユーザ名
		((TextView) findViewById(R.id.item_detail_user_name)).setText(name);
		// 投稿時間
		Date createdAt = tweets.getCreatedAt();
		String date = dateFormat.format(createdAt) + " " + timeFormat.format(createdAt);
		((TextView) findViewById(R.id.item_detail_time)).setText(date);
		// 位置情報
		String address = AddressCache.getAddress(tweets.getLatitude() + "," + tweets.getLongitude());
		if (address != null) {
			TextView locationView = (TextView) findViewById(R.id.item_detail_location);
			locationView.setText(address);
			locationView.setVisibility(View.VISIBLE);
		}
		// ハッシュタグを適応して設定
		TextView textView = (TextView) findViewById(R.id.item_detail_text);
		textView.setText(applyHashTag(tweets.getText()));
		textView.setMovementMethod(LinkMovementMethod.getInstance());
		// ユーザー詳細
		findViewById(R.id.user_detail).setOnClickListener(this);
		// 返信ボタン
		((Button) findViewById(R.id.btn_detail_reply)).setOnClickListener(this);
		// フォローボタン
		followButton = (Button) findViewById(R.id.btn_detail_follow);
		followButton();
		// Toast生成
		createToast();
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.user_detail:
			Toast.makeText(this, "ID:"+tweets.getUserId(), Toast.LENGTH_SHORT).show();
			Intent userDetailIntent = new Intent(this, UserDetail.class);
			userDetailIntent.putExtra(UserDetail.PARAM_USER_NAME, tweets.getScreenName());
			startActivity(userDetailIntent);
			break;
		case R.id.btn_detail_reply:
			Intent intent = new Intent(this, TweetMain.class);
			intent.putExtra(PARAM_SEND_TYPE, TYPE_REPLY);
			intent.putExtra(PARAM_REPLY_NAME, tweets.getScreenName());
			startActivity(intent);
			break;
		case R.id.btn_detail_follow:
			if (isFollows) {
				showDialog(DIALOG_CONFIRM_RELEASE_FOLLOW);
			} else {
				showDialog(DIALOG_CONFIRM_FOLLOW);
			}
		}
	}

	private void followButton() {
		(new AsyncTask<Long, Integer, Boolean>() {
			@Override
			protected Boolean doInBackground(Long... ids) {
				try {
					isFollows = twitter.existsFriendship(
							String.valueOf(twitter.getId()),
							String.valueOf(tweets.getUserId()));
				} catch (IllegalStateException e) {
				} catch (TwitterException e) {
				}
				return isFollows;
			}

			@Override
			protected void onPostExecute(Boolean result) {
				setFollowButton();
				followButton.setOnClickListener(TweetDetail.this);
				followButton.setEnabled(true);
			}
		}).execute();
	}
	private void setFollowButton() {
		if (isFollows) {
			followButton.setText(getString(R.string.btn_detail_release_follow));
		} else {
			followButton.setText(getString(R.string.btn_detail_follow));
		}
	}
	private void createToast() {
		followSuccess = createToast(getString(R.string.toast_detail_follow_success));
		releaseFollowSucess = createToast(getString(R.string.toast_detail_release_follow_success));
		followFailed = createToast(getString(R.string.toast_detail_follow_failed));
		releasefollowFailed = createToast(getString(R.string.toast_detail_release_follow_failed));
	}
	private Toast createToast(String msg) {
		Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER, 0, 0);
		return toast;
	}
	private void follow() {
		try {
			twitter.createFriendship(tweets.getUserId());
			followSuccess.show();
			isFollows = true;
			setFollowButton();
		} catch (TwitterException e) {
			followFailed.show();
		}
	}
	private void releaseFollow() {
		try {
			twitter.destroyFriendship(tweets.getUserId());
			releaseFollowSucess.show();
			isFollows = false;
			setFollowButton();
		} catch (TwitterException e) {
			releasefollowFailed.show();
		}
	}
	/**
	 * ハッシュタグの適用
	 * @param text
	 * @return
	 */
	private Spannable applyHashTag(CharSequence text) {
		Spannable spannable = Spannable.Factory.getInstance().newSpannable(text);
		Matcher matcher = hashtagPattern.matcher(text);
		while(matcher.find()) {
			spannable.setSpan(
					new HashTagClickable(this, matcher.group(1)),
					matcher.start(1),
					matcher.end(1),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		return spannable;
	}


	@Override
	protected Dialog onCreateDialog(int id, Bundle bundle) {
		switch (id) {
		case DIALOG_CONFIRM_FOLLOW:
			return new AlertDialog.Builder(this)
			.setMessage(getString(R.string.detail_confirm_follow))
			.setPositiveButton(getString(R.string.detail_yes), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					follow();
				}
			})
			.setNegativeButton(getString(R.string.detail_no), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			}).create();
		case DIALOG_CONFIRM_RELEASE_FOLLOW:
			return new AlertDialog.Builder(this)
			.setMessage(getString(R.string.detail_confirm_release_follow))
			.setPositiveButton(getString(R.string.detail_yes), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					releaseFollow();
				}
			})
			.setNegativeButton(getString(R.string.detail_no), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			}).create();
		}
		return super.onCreateDialog(id, bundle);
	}

	
}
