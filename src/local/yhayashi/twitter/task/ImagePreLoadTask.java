package local.yhayashi.twitter.task;

import java.io.IOException;
import java.net.URL;

import local.yhayashi.twitter.common.ImageCache;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

public class ImagePreLoadTask extends AsyncTask<URL, Integer, Bitmap> {

	@Override
	protected Bitmap doInBackground(URL... url) {
		String key = url[0].toString();
		Bitmap bitmap = ImageCache.getImage(key);
		if (bitmap != null) {
			return bitmap;
		}
		try {
			bitmap = BitmapFactory.decodeStream(url[0].openStream());
			ImageCache.setImage(key, bitmap);
		} catch (IOException e) {
		}
		return bitmap;
	}

}
