package local.yhayashi.twitter.db;

import java.io.Serializable;
import java.util.Date;

public class TweetsInfo implements Serializable {

	/**
	 * シリアルバージョンUID
	 */
	private static final long serialVersionUID = 1L;

	private int _id;
	private String listName;
	private long statusId;
	private int index;
	private long userId;
	private String screenName;
	private String name;
	private String text;
	private Date createdAt;
	private String profileImageURL;
	private byte[] bitmap;
	private double latitude = -1.0;
	private double longitude = -1.0;

	public int getId() {
		return _id;
	}
	public String getListName() {
		return listName;
	}
	public long getStatusId() {
		return statusId;
	}
	public long getUserId() {
		return userId;
	}
	public int getIndex() {
		return index;
	}
	public String getScreenName() {
		return screenName;
	}
	public String getName() {
		return name;
	}
	public String getText() {
		return text;
	}
	public Date getCreatedAt() {
		return createdAt;
	}
	public String getProfileImageURL() {
		return profileImageURL;
	}
	public byte[] getBitmap() {
		return bitmap;
	}
	public double getLatitude() {
		return latitude;
	}
	public double getLongitude() {
		return longitude;
	}

	public void setId(int _id) {
		this._id = _id;
	}
	public void setListName(String listName) {
		this.listName = listName;
	}
	public void setStatusId(long statusId) {
		this.statusId = statusId;
	}
	public void setUserId(long userId) {
		this.userId = userId;
	}
	public void setIndex(int index) {
		this.index = index;
	}
	public void setScreenName(String screenName) {
		this.screenName = screenName;
	}
	public void setName(String name) {
		this.name = name;
	}
	public void setText(String text) {
		this.text = text;
	}
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}
	public void setProfileImageURL(String profileImageURL) {
		this.profileImageURL = profileImageURL;
	}
	public void setBitmap(byte[] bitmap) {
		this.bitmap = bitmap;
	}
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
}