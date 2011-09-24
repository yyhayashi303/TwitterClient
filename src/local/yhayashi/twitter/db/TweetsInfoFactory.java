package local.yhayashi.twitter.db;

import twitter4j.GeoLocation;
import twitter4j.Tweet;
import twitter4j.User;

public class TweetsInfoFactory {

	/**
	 * TweetからTweetsInfoを生成して返します。
	 * @param tweet
	 * @return
	 */
	public static TweetsInfo createTweetsInfo(Tweet tweet) {
		TweetsInfo info = new TweetsInfo();
		info.setStatusId(tweet.getId());
		info.setUserId(tweet.getFromUserId());
		info.setScreenName(tweet.getFromUser());
		info.setText(tweet.getText());
		info.setProfileImageURL(tweet.getProfileImageUrl());
		info.setCreatedAt(tweet.getCreatedAt());
		GeoLocation geoLocation = tweet.getGeoLocation();
		if (geoLocation != null) {
			info.setLatitude(geoLocation.getLatitude());
			info.setLongitude(geoLocation.getLongitude());
		}
		return info;
	}

	/**
	 * twitter4j.StatusからTweetsInfoを生成して返します。
	 * @param status
	 * @return
	 */
	public static TweetsInfo createTweetsInfo(twitter4j.Status status) {
		TweetsInfo info = new TweetsInfo();
		User user = status.getUser();
		info.setStatusId(status.getId());
		info.setUserId(user.getId());
		info.setScreenName(user.getScreenName());
		info.setName(user.getName());
		info.setText(status.getText());
		info.setProfileImageURL(user.getProfileImageURL().toString());
		info.setCreatedAt(status.getCreatedAt());
		GeoLocation geoLocation = status.getGeoLocation();
		if (geoLocation != null) {
			info.setLatitude(geoLocation.getLatitude());
			info.setLongitude(geoLocation.getLongitude());
		}
		return info;
	}

}
