package local.yhayashi.twitter.task;

import java.io.IOException;
import java.net.URL;

import local.yhayashi.twitter.common.ImageCache;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

public class ImagePreLoadTask2 extends AsyncTask<URL, Integer, Bitmap> {

	@Override
	protected Bitmap doInBackground(URL ... urls) {
		for (URL url : urls) {
			try {
				ImageCache.setImage(url.toString(),
						BitmapFactory.decodeStream(url.openStream()));
			} catch (IOException e) {
			}
		}
		return null;
	}

}
