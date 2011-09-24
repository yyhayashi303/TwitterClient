package local.yhayashi.twitter.db;

import android.content.Context;

public class TimelineCacheDaoFactory {
	/** シングルトン */
	private static final TimelineCacheDaoFactory factory = new TimelineCacheDaoFactory();
	private TimelineCacheDaoFactory() {}

	/**
	 * ファクトリのインスタンスを返します。
	 * @return
	 */
	public static TimelineCacheDaoFactory getFactory() {
		return factory;
	}

	/**
	 * DAOを生成して返します。
	 * @param context
	 * @return
	 */
	public TimelineCacheDao getTimelineCacheDao(Context context) {
		return new TimelineCacheDao(new DatabaseHelper(context));
	}
}
