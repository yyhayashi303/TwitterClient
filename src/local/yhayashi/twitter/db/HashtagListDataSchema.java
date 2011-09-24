package local.yhayashi.twitter.db;

import android.database.sqlite.SQLiteDatabase;

/**
 * ハッシュタグ一覧テーブル定義
 * 
 */
public final class HashtagListDataSchema {
	/** テーブル名 */
	public static final String TABLE_NAME = "hashtag_list";

	/** カラム名：ID */
	public static final String COLUMN_ID = "_id";
	/** カラム名：ハッシュタグ名 */
	public static final String COLUMN_NAME = "hashtag_name";

	/** 全カラムの配列 */
	public static final String[] ALL_COLUMN = {
		COLUMN_ID,
		COLUMN_NAME
	};
	/** SQL：CREATE TABLE */
	private static final String SQL_CREATE_TABLE =
		"CREATE TABLE " + TABLE_NAME + " ("
		+ COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT"
		+ "," + COLUMN_NAME + " TEXT"
		+ ")";
	/** SQL：DROP_TABLE */
	private static final String SQL_DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;

	/**
	 * コンストラクタは使用しません。
	 */
	private HashtagListDataSchema() { }

	/**
	 * このテーブルを作成します。
	 * @param db SQLiteDatabase
	 */
	public static void createTable(SQLiteDatabase db) {
		db.execSQL(SQL_CREATE_TABLE);
	}

	/**
	 * このテーブルをドロップします。
	 * @param db SQLiteDatabase
	 */
	public static void dropTable(SQLiteDatabase db) {
		db.execSQL(SQL_DROP_TABLE);
	}
	public static void initialize(SQLiteDatabase db) {
		android.content.ContentValues value1 = new android.content.ContentValues();
		android.content.ContentValues value2 = new android.content.ContentValues();
		android.content.ContentValues value3 = new android.content.ContentValues();
		android.content.ContentValues value4 = new android.content.ContentValues();
		value1.put(COLUMN_NAME, "#nhk_news");
		value2.put(COLUMN_NAME, "#javascript");
		value3.put(COLUMN_NAME, "#てすと");
		value4.put(COLUMN_NAME, "#経済");
		db.insert(TABLE_NAME, null, value1);
		db.insert(TABLE_NAME, null, value2);
		db.insert(TABLE_NAME, null, value3);
		db.insert(TABLE_NAME, null, value4);
	}
}
