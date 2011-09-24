package local.yhayashi.twitter.db;

import static local.yhayashi.twitter.db.TimelineCacheSchema.ALL_COLUMN;

import static local.yhayashi.twitter.db.TimelineCacheSchema.COLUMN_CREATED_AT;
import static local.yhayashi.twitter.db.TimelineCacheSchema.*;
import static local.yhayashi.twitter.db.TimelineCacheSchema.COLUMN_ICON_URL;
import static local.yhayashi.twitter.db.TimelineCacheSchema.COLUMN_ID;
import static local.yhayashi.twitter.db.TimelineCacheSchema.COLUMN_INDEX;
import static local.yhayashi.twitter.db.TimelineCacheSchema.COLUMN_LATITUDE;
import static local.yhayashi.twitter.db.TimelineCacheSchema.COLUMN_LIST_NAME;
import static local.yhayashi.twitter.db.TimelineCacheSchema.COLUMN_LONGITUDE;
import static local.yhayashi.twitter.db.TimelineCacheSchema.COLUMN_SCREEN_NAME;
import static local.yhayashi.twitter.db.TimelineCacheSchema.COLUMN_TEXT;
import static local.yhayashi.twitter.db.TimelineCacheSchema.COLUMN_USER_ID;
import static local.yhayashi.twitter.db.TimelineCacheSchema.TABLE_NAME;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class TimelineCacheDao {

	/** DBヘルパー */
	private final DatabaseHelper helper;

	TimelineCacheDao(DatabaseHelper helper) {
		this.helper = helper;
	}

	/**
	 * 指定リスト名のデータを検索して返します。
	 * @param listName
	 * @param orderBy
	 * @return
	 */
	public List<TweetsInfo> findList(String listName, String orderBy) {
		return query(ALL_COLUMN, COLUMN_LIST_NAME + "='" + listName + "'", null, null, null, orderBy);
	}

	/**
	 * DTOのリストを登録します。
	 * @param dtoList
	 */
	public void insert(List<TweetsInfo> dtoList) {
		if (dtoList.size() < 1) {
			return;
		}
		SQLiteDatabase db = helper.getWritableDatabase();
		for (TweetsInfo dto : dtoList) {
			db.insert(TABLE_NAME, null, createContentValue(dto));
		}
	}

	/**
	 * 対象リスト名のデータを削除します。
	 * @param listName
	 * @return 影響を受けた行数
	 */
	public int deleteList(String listName) {
		SQLiteDatabase db = helper.getWritableDatabase();
		return db.delete(TABLE_NAME, COLUMN_LIST_NAME + "='" + listName + "'", null);
	}

	/**
	 * 指定の条件でBMI履歴テーブルを検索した結果を返します。
	 * @param columns
	 * @param selection
	 * @param selectionArgs
	 * @param groupBy
	 * @param having
	 * @param orderBy
	 * @return カーソル
	 */
	public List<TweetsInfo> query(String[] columns,
			String selection,
			String[] selectionArgs,
			String groupBy,
			String having,
			String orderBy) {
		SQLiteDatabase db = helper.getWritableDatabase();
		return createTimelineCache(db.query(TABLE_NAME, columns, selection, selectionArgs, groupBy, having, orderBy));
	}

	/**
	 * DTOの値を設定したContentValuesを返します。
	 * @param dto DTO
	 * @return ContentValues
	 */
	private ContentValues createContentValue(TweetsInfo dto) {
		ContentValues values = new ContentValues();
		values.put(COLUMN_STATUS_ID, dto.getStatusId());
		values.put(COLUMN_LIST_NAME, dto.getListName());
		values.put(COLUMN_INDEX, dto.getIndex());
		values.put(COLUMN_USER_ID, dto.getUserId());
		values.put(COLUMN_SCREEN_NAME, dto.getScreenName());
		values.put(COLUMN_USER_NAME, dto.getName());
		values.put(COLUMN_TEXT, dto.getText());
		values.put(COLUMN_CREATED_AT, dto.getCreatedAt().getTime());
		values.put(COLUMN_ICON_URL, dto.getProfileImageURL());
		values.put(COLUMN_ICON_DATA, dto.getBitmap());
		values.put(COLUMN_LATITUDE, dto.getLatitude());
		values.put(COLUMN_LONGITUDE, dto.getLongitude());
		return values;
	}

	/**
	 * カーソルのデータをDTOに設定してListとして返します。
	 * @param cursor
	 * @return DTOのリスト
	 */
	private List<TweetsInfo> createTimelineCache(Cursor cursor) {
		List<TweetsInfo> dtoList = new ArrayList<TweetsInfo>();
		while (cursor.moveToNext()) {
			TweetsInfo dto = new TweetsInfo();
			dto.setId(cursor.getInt(cursor.getColumnIndex(COLUMN_ID)));
			dto.setStatusId(cursor.getLong(cursor.getColumnIndex(COLUMN_STATUS_ID)));
			dto.setListName(cursor.getString(cursor.getColumnIndex(COLUMN_LIST_NAME)));
			dto.setIndex(cursor.getInt(cursor.getColumnIndex(COLUMN_INDEX)));
			dto.setUserId(cursor.getInt(cursor.getColumnIndex(COLUMN_USER_ID)));
			dto.setScreenName(cursor.getString(cursor.getColumnIndex(COLUMN_SCREEN_NAME)));
			dto.setName(cursor.getString(cursor.getColumnIndex(COLUMN_USER_NAME)));
			dto.setText(cursor.getString(cursor.getColumnIndex(COLUMN_TEXT)));
			dto.setCreatedAt(new Date(cursor.getLong(cursor.getColumnIndex(COLUMN_CREATED_AT))));
			dto.setProfileImageURL(cursor.getString(cursor.getColumnIndex(COLUMN_ICON_URL)));
			dto.setBitmap(cursor.getBlob(cursor.getColumnIndex(COLUMN_ICON_DATA)));
			dto.setLatitude(cursor.getDouble(cursor.getColumnIndex(COLUMN_LATITUDE)));
			dto.setLongitude(cursor.getDouble(cursor.getColumnIndex(COLUMN_LONGITUDE)));
			dtoList.add(dto);
		}
		cursor.close();
		return dtoList;
	}

	/**
	 * 接続を閉じます。
	 */
	public void close() {
		helper.close();
	}
}
