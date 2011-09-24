package local.yhayashi.twitter.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public final class DatabaseHelper extends SQLiteOpenHelper {

	/** データベース名 */
	private static final String DATABASE_NAME = "twitter_client";
	/** データベースバージョン */
	private static final int DATABASE_VERSION = 1;

	/**
	 * インスタンスを生成します。
	 * @param context
	 */
	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// タイムラインキャッシュテーブル生成
		TimelineCacheSchema.createTable(db);
		// ハッシュタグ一覧テーブル生成
		HashtagListDataSchema.createTable(db);

		/*
		 * 端末配布用に初期データ設定
		 */
		HashtagListDataSchema.initialize(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// テーブルに変更がある場合に使用
	}
}
