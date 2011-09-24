package local.yhayashi.twitter.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class TwitterOAuth extends Activity {
	/** コールバックURL */
	public static final String CALLBACK_URL = "https://sites.google.com/site/cyhayashi303twitterclient/";
	/** HTTPパラメータ oauth_token */
	public static final String HTTP_PARAM_OAUTH_TOKEN = "oauth_token";
	/** HTTPパラメータoauth_verifier */
	public static final String HTTP_PARAM_OAUTH_VERIFIER = "oauth_verifier";
	/** OAuth URL */
	public static final String PARAM_OAUTH_URL = "auth_url";

	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.oauth);

		// アプリ承認ページ表示用
		WebView webView = (WebView) findViewById(R.id.oauth_web_view);
		webView.setWebViewClient(new WebViewClient(){
			/**
			 * コールバックに飛んだ際にHTTPのリクエストパラメータからoauth_tokenとoauth_verifierを取得して返す。
			 */
			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				super.onPageStarted(view, url, favicon);
				if (url != null && url.startsWith(CALLBACK_URL)) {
					url.split("\\?")[1].split("&");
					String[] urlParameters = url.split("\\?")[1].split("&");
					int urlParametersLen = urlParameters.length;
					String oauthToken = null;
					String oauthVerifier = null;

					if (0 < urlParametersLen
							&& urlParameters[0].startsWith(HTTP_PARAM_OAUTH_TOKEN)){
						oauthToken = urlParameters[0].split("=")[1];
					} else if (1 < urlParametersLen
							&& urlParameters[1].startsWith(HTTP_PARAM_OAUTH_TOKEN)){
						oauthToken = urlParameters[1].split("=")[1];
					}

					if (0 < urlParametersLen
							&& urlParameters[0].startsWith(HTTP_PARAM_OAUTH_VERIFIER)){
						oauthVerifier = urlParameters[0].split("=")[1];
					} else if (1 < urlParametersLen
							&& urlParameters[1].startsWith(HTTP_PARAM_OAUTH_VERIFIER)){
						oauthVerifier = urlParameters[1].split("=")[1];
					}

					if (oauthToken != null && oauthVerifier != null) {
						Intent intent = new Intent();
						intent.putExtra(HTTP_PARAM_OAUTH_TOKEN, oauthToken);
						intent.putExtra(HTTP_PARAM_OAUTH_VERIFIER, oauthVerifier);
						setResult(Activity.RESULT_OK, intent);
						finish();
					}
				}
			}
		});
		// 認証ページ表示
		webView.loadUrl(this.getIntent().getExtras().getString(PARAM_OAUTH_URL));
	}
}