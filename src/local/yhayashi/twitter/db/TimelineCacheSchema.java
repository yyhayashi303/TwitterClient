package local.yhayashi.twitter.db;

import android.database.sqlite.SQLiteDatabase;

/**
 * BMI履歴テーブル定義
 * 
 */
public final class TimelineCacheSchema {
	/** テーブル名 */
	public static final String TABLE_NAME = "timeline_cache";

	/** カラム名：ID */
	public static final String COLUMN_ID = "_id";
	/** カラム名：ステータスID */
	public static final String COLUMN_STATUS_ID = "status_id";
	/** カラム名：リスト名 */
	public static final String COLUMN_LIST_NAME = "list_name";
	/** カラム名：インデックス */
	public static final String COLUMN_INDEX = "adapter_index";
	/** カラム名：ユーザID */
	public static final String COLUMN_USER_ID = "user_id";
	/** カラム名：スクリーン名前 */
	public static final String COLUMN_SCREEN_NAME = "screen_name";
	/** カラム名：ユーザ名 */
	public static final String COLUMN_USER_NAME = "user_name";
	/** カラム名：テキスト */
	public static final String COLUMN_TEXT = "tweets_text";
	/** カラム名：日時 */
	public static final String COLUMN_CREATED_AT = "created_at";
	/** カラム名：アイコン画像URL */
	public static final String COLUMN_ICON_URL = "icon_url";
	/** カラム名：アイコン画像 */
	public static final String COLUMN_ICON_DATA = "icon_data";
	/** カラム名：緯度 */
	public static final String COLUMN_LATITUDE = "latitude";
	/** カラム名：経度 */
	public static final String COLUMN_LONGITUDE = "longitude";

	/** 全カラムの配列 */
	public static final String[] ALL_COLUMN = {
		COLUMN_ID,
		COLUMN_STATUS_ID,
		COLUMN_LIST_NAME,
		COLUMN_INDEX,
		COLUMN_USER_ID,
		COLUMN_SCREEN_NAME,
		COLUMN_USER_NAME,
		COLUMN_TEXT,
		COLUMN_CREATED_AT,
		COLUMN_ICON_URL,
		COLUMN_ICON_DATA,
		COLUMN_LATITUDE,
		COLUMN_LONGITUDE
	};
	/** SQL：CREATE TABLE */
	private static final String SQL_CREATE_TABLE =
		"CREATE TABLE " + TABLE_NAME + " ("
		+ COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT"
		+ "," + COLUMN_STATUS_ID + " INTEGER"
		+ "," + COLUMN_LIST_NAME + " TEXT"
		+ "," + COLUMN_INDEX + " INTEGER"
		+ "," + COLUMN_USER_ID + " INTEGER"
		+ "," + COLUMN_SCREEN_NAME + " TEXT"
		+ "," + COLUMN_USER_NAME + " TEXT"
		+ "," + COLUMN_TEXT + " TEXT"
		+ "," + COLUMN_CREATED_AT + " TEXT"
		+ "," + COLUMN_ICON_URL + " TEXT"
		+ "," + COLUMN_ICON_DATA + " BLOB"
		+ "," + COLUMN_LATITUDE + " REAL"
		+ "," + COLUMN_LONGITUDE + " REAL"
		+ ")";
	/** SQL：DROP_TABLE */
	private static final String SQL_DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;

	/**
	 * コンストラクタは使用しません。
	 */
	private TimelineCacheSchema() { }

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
}
