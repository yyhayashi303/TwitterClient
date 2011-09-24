package local.yhayashi.twitter.activity;

import local.yhayashi.twitter.common.HashtagListAdapter;
import twitter4j.Twitter;
import android.app.Application;

/**
 * アプリ内データ共有Bean
 */
public class TwitterClientBean extends Application {
	/** Twitterオブジェクト */
	private Twitter twitter;
	/** ハッシュタグリストアダプタ */
	private HashtagListAdapter hashtagListAdapter;

	public Twitter getTwitter() {
		return twitter;
	}
	public void setTwitter(Twitter twitter) {
		this.twitter = twitter;
	}
	public HashtagListAdapter getHashtagListAdapter() {
		return hashtagListAdapter;
	}
	public void setHashtagListAdapter(HashtagListAdapter hashtagListAdapter) {
		this.hashtagListAdapter = hashtagListAdapter;
	}
}
