package local.yhayashi.twitter.db;

import static local.yhayashi.twitter.db.HashtagListDataSchema.ALL_COLUMN;
import static local.yhayashi.twitter.db.HashtagListDataSchema.COLUMN_ID;
import static local.yhayashi.twitter.db.HashtagListDataSchema.COLUMN_NAME;
import static local.yhayashi.twitter.db.HashtagListDataSchema.TABLE_NAME;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class HashtagListDataDao {
	private final DatabaseHelper helper;
	HashtagListDataDao(DatabaseHelper helper) {
		this.helper = helper;
	}

	/**
	 * 指定リスト名のデータを検索して返します。
	 * @param listName
	 * @param orderBy
	 * @return
	 */
	public List<HashtagListData> findAll(String orderBy) {
		return query(ALL_COLUMN, null, null, null, null, orderBy);
	}

	/**
	 * DTOのリストを登録します。
	 * @param dtoList
	 */
	public void insert(HashtagListData dto) {
		SQLiteDatabase db = helper.getWritableDatabase();
		db.insert(TABLE_NAME, null, createContentValue(dto));
	}

	/**
	 * 対象のデータを削除します。
	 * @param name
	 * @return 影響を受けた行数
	 */
	public int delete(String name) {
		SQLiteDatabase db = helper.getWritableDatabase();
		return db.delete(TABLE_NAME, COLUMN_NAME + "='" + name + "'", null);
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
	public List<HashtagListData> query(String[] columns,
			String selection,
			String[] selectionArgs,
			String groupBy,
			String having,
			String orderBy) {
		SQLiteDatabase db = helper.getWritableDatabase();
		return createHashtagList(db.query(TABLE_NAME, columns, selection, selectionArgs, groupBy, having, orderBy));
	}

	/**
	 * DTOの値を設定したContentValuesを返します。
	 * @param dto DTO
	 * @return ContentValues
	 */
	private ContentValues createContentValue(HashtagListData dto) {
		ContentValues values = new ContentValues();
		values.put(COLUMN_NAME, dto.getName());
		return values;
	}

	/**
	 * カーソルのデータをDTOに設定してListとして返します。
	 * @param cursor
	 * @return DTOのリスト
	 */
	private List<HashtagListData> createHashtagList(Cursor cursor) {
		List<HashtagListData> dtoList = new ArrayList<HashtagListData>();
		while (cursor.moveToNext()) {
			HashtagListData dto = new HashtagListData();
			dto.setId(cursor.getInt(cursor.getColumnIndex(COLUMN_ID)));
			dto.setName(cursor.getString(cursor.getColumnIndex(COLUMN_NAME)));
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
