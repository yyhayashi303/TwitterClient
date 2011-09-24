package local.yhayashi.twitter.common;

import local.yhayashi.twitter.activity.HashTagMain;
import android.content.Context;
import android.content.Intent;
import android.text.style.ClickableSpan;
import android.view.View;

/**
 * ハッシュタグクリックイベント
 */
public class HashTagClickable extends ClickableSpan {
	private final Context context;
	private final String hashTag;
	public HashTagClickable(Context context, String hashTag) {
		this.context = context;
		this.hashTag = hashTag;
	}

	/**
	 * クリックされたハッシュタグのリストを検索して表示します。
	 */
	@Override
	public void onClick(View widget) {
		Intent intent = new Intent(context, HashTagMain.class);
		intent.putExtra(HashTagMain.PARAM_HASHTAG, hashTag);
		context.startActivity(intent);
	}

}
