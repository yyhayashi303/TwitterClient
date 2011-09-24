package local.yhayashi.twitter.common;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 住所キャッシュ
 */
public class AddressCache {
	private static ConcurrentHashMap<String, String> addressCache =
		new ConcurrentHashMap<String, String>();

	private AddressCache() {}

	public static String getAddress(String key) {
		return addressCache.get(key);
	}
	public static void setAddress(String key, String address) {
		if (key == null || address == null) {
			return;
		}
		if (addressCache.get(key) == null) {
			addressCache.putIfAbsent(key, address);
		}
	}
}
