package local.yhayashi.twitter.common;

import java.util.concurrent.ConcurrentHashMap;

import android.graphics.Bitmap;

/**
 * 画像キャッシュ
 */
public final class ImageCache {
	private static ConcurrentHashMap<String, Bitmap> imageCache =
		new ConcurrentHashMap<String, Bitmap>();

	private ImageCache() {}

	public static Bitmap getImage(String key) {
		return imageCache.get(key);
	}
	public static void setImage(String key, Bitmap image) {
		if (key == null || image == null) {
			return;
		}
		if (imageCache.get(key) == null) {
			imageCache.putIfAbsent(key, image);
		}
	}
}
