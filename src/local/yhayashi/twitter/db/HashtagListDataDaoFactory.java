package local.yhayashi.twitter.db;

import android.content.Context;

public class HashtagListDataDaoFactory {
	/** シングルトン */
	private static final HashtagListDataDaoFactory factory = new HashtagListDataDaoFactory();
	private HashtagListDataDaoFactory() {}

	/**
	 * ファクトリのインスタンスを返します。
	 * @return
	 */
	public static HashtagListDataDaoFactory getFactory() {
		return factory;
	}

	/**
	 * DAOを生成して返します。
	 * @param context
	 * @return
	 */
	public HashtagListDataDao getHashtagListDao(Context context) {
		return new HashtagListDataDao(new DatabaseHelper(context));
	}
}
