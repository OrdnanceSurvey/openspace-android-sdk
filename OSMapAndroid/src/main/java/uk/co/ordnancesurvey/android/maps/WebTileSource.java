/**
 * OpenSpace Android SDK Licence Terms
 *
 * The OpenSpace Android SDK is protected by © Crown copyright – Ordnance Survey 2013.[https://github.com/OrdnanceSurvey]
 *
 * All rights reserved (subject to the BSD licence terms as follows):.
 *
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * Neither the name of Ordnance Survey nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 *
 */
package uk.co.ordnancesurvey.android.maps;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.net.http.AndroidHttpClient;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

abstract class WebTileSource extends OSTileSource {
	private final static String TAG = "WebTileSource";

	public WebTileSource(String[] productsOrNull) {
		super(productsOrNull);

		// There's a bug in Android versions prior to 2.2 (Froyo, API level 8):
		// * http://android-developers.blogspot.co.uk/2011/09/androids-http-clients.html
		// * https://code.google.com/p/android/issues/detail?id=2939
		// Our current minimum is 2.2.3 (API level 10), but an assert does not hurt in case an app includes this in a JAR.
		assert Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO : "Android < 2.2 is not supported (Android issue #2939)";
	}

	abstract String uriStringForTile(MapTile tile);

	@Override
	byte[] dataForTile(MapTile tile) {
		String uriString = uriStringForTile(tile);
		if (uriString == null)
		{
			return null;
		}
		// The compiler is clever enough to detect that it will never be used uninitialized.
		final long startTime, startThreadTime;
		if (BuildConfig.DEBUG) {
			startTime = SystemClock.uptimeMillis();
			startThreadTime = SystemClock.currentThreadTimeMillis();
		} else {
            startTime = 0;
            startThreadTime = 0;
        }

		boolean success = false;
		try {
			byte[] ret = loadDataWithHttpURLConnection(uriString);
			//byte[] ret = loadDataWithAndroidHttpClient(uriString);
			//byte[] ret = loadDatapWithDefaultHttpClient(uriString);
			success = true;
			return ret;
		} finally {
			// Do this here so it happens on the majority of return paths. It also happens on exception paths, which is acceptable.
			if (BuildConfig.DEBUG)
			{
				long endTime = SystemClock.uptimeMillis();
				long endThreadTime = SystemClock.currentThreadTimeMillis();
				Log.v(TAG, String.format(Locale.ENGLISH, "Fetch %s %d ms (real), %d ms (CPU)", (success?"took":"failed,"), endTime-startTime, endThreadTime-startThreadTime));
			}
		}
	}

	private byte[] loadDataWithHttpURLConnection(String uriString)
	{
		URL url;
		try {
			url = new URL(uriString);
		} catch (MalformedURLException e) {
			throw new Error("Caught MalformedURLException where it should never happen", e);
		}

		HttpURLConnection urlConnection = null;
		try {
			urlConnection = (HttpURLConnection)url.openConnection();

			// tchan: It is not worth using a BufferedInputStream; the vast majority of the CPU time is spent before getInputStream() returns.
			// According to the Android docs, it is not our job to close the stream:
			//   http://developer.android.com/reference/java/net/HttpURLConnection.html
			InputStream inputStream = urlConnection.getInputStream();

			// We can only get the response code after getInputStream() returns.
			//   http://www.tbray.org/ongoing/When/201x/2012/01/17/HttpURLConnection
			int httpStatusCode = urlConnection.getResponseCode();
			// What should we do about errors?
			if (httpStatusCode/100 != 2) {
				if (BuildConfig.DEBUG)
				{
					assert false;
				}
				//Log.v(TAG, String.format(Locale.ENGLISH, "Request for tile TILEMATRIX=%s TILEROW=%d TILECOL=%d returned HTTP status %d", wmtsCode, tileRow, tileCol, httpStatusCode));
				return null;
			}

			// We do not need to close the stream according to http://developer.android.com/reference/java/net/HttpURLConnection.html
			// The Java docs are unclear: http://docs.oracle.com/javase/6/docs/api/java/net/URLConnection.html
			return Helpers.readAllNoClose(inputStream);
		} catch (IOException e) {
			Log.v(TAG, "Failed to fetch tile", e);
			return null;
		} finally {
			if (urlConnection != null) {
				// According to http://developer.android.com/reference/java/net/HttpURLConnection.html
				//   "Once the response body has been read, the HttpURLConnection should be closed by calling disconnect()."
				//   "Unlike other Java implementations, this will not necessarily close socket connections that can be reused. "
				urlConnection.disconnect();
			}
		}
	}

	@SuppressWarnings("unused")
	private byte[] loadDataWithAndroidHttpClient(String uriString)
	{
		// This is largely a test to see if it will improve upon HttpURLConnection.
		// If anything, AndroidHttpClient seems to have slightly more overhead:
		//   loadBitmapWithHttpURLConnection() spends about
		//     30.3% in BitmapFactory.decodeStream()
		//     12.5% in URL.<init>
		//     57.1% in HttpURLConnectionImpl.getInputStream()
		//   loadBitmapWithAndroidHttpClient() spends about
		//     28.4% in BitmapFactory.decodeStream()
		//     17.0% in HttpGet.<init>
		//     54.6% in AndroidHttpClient.execute()
		// We might be able to make AndroidHttpClient marginally faster if we can reuse HttpGet objects, but that does not seem worth it.

		// AndroidHttpClient seems to have a lower limit on the number of concurrent connections per host, which makes scrolling appear smother.
		// A similar effect can be achieved by simply limiting the number of request threads.

		// TODO: Version number.
		AndroidHttpClient client = AndroidHttpClient.newInstance("OSMapAndroid/0.1");
		try {
			HttpGet request = new HttpGet(uriString);
			InputStream inputStream = client.execute(request).getEntity().getContent();
			// We should close the stream according to http://hc.apache.org/httpcomponents-client-ga/tutorial/html/fundamentals.html#d5e37
			return Helpers.readAllAndClose(inputStream);
		} catch (IOException e) {
			Log.v(TAG, "AndroidHttpClient.execute() threw", e);
			return null;
		} finally {
			client.close();
		}
	}

	@SuppressWarnings("unused")
	private byte[] loadDataWithDefaultHttpClient(String uriString)
	{
		DefaultHttpClient client = new DefaultHttpClient();
		try {
			HttpGet request = new HttpGet(uriString);
			InputStream inputStream = client.execute(request).getEntity().getContent();
			// We should close the stream according to http://hc.apache.org/httpcomponents-client-ga/tutorial/html/fundamentals.html#d5e37
			return Helpers.readAllAndClose(inputStream);
		} catch (IOException e) {
			Log.v(TAG, "AndroidHttpClient.execute() threw", e);
			return null;
		}
	}


	@Override
	boolean isNetwork() {
		return true;
	}

	@Override
	boolean isSynchronous() {
		return false;
	}

	@Override
	boolean shouldDiskCache() {
		return true;
	}
}
