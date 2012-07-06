/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package nl.adben.android.rssreader;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Main Activity for the sample application.
 * <p/>
 * This activity does the following:
 * <p/>
 * o Presents a WebView screen to users. This WebView has a list of HTML links
 * to the latest questions tagged 'android' on stackoverflow.com.
 * <p/>
 * o Parses the StackOverflow XML feed using XMLPullParser.
 * <p/>
 * o Uses AsyncTask to download and process the XML feed.
 * <p/>
 * o Monitors preferences and the device's network connection to determine
 * whether to refresh the WebView content.
 */
public class RssReaderApp extends ListActivity {
	public static final String WIFI = "Wi-Fi";
	public static final String ANY = "Any";
	private static final String URL = "http://news.ycombinator.com/rss";
	private static final String APP_TAG = "NetworkActivity debug";
	private static final String LISTENER_TAG = "TextViewListener";


	// Whether there is a Wi-Fi connection.
	private static boolean wifiConnected = false;
	// Whether there is a mobile connection.
	private static boolean mobileConnected = false;
	// Whether the display should be refreshed.
	private static boolean refreshDisplay = true;
	// The user's current network preference setting.
	private static String sPref = null;
	// The BroadcastReceiver that tracks network connectivity changes.
	private NetworkReceiver receiver = new NetworkReceiver();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Register BroadcastReceiver to track connection changes.
		IntentFilter filter = new IntentFilter(
				ConnectivityManager.CONNECTIVITY_ACTION);
		receiver = new NetworkReceiver();
		this.registerReceiver(receiver, filter);
	}

	// Refreshes the display if the network connection and the
	// pref settings allow it.
	@Override
	public void onStart() {
		super.onStart();

		// Gets the user's network preference settings
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		// Retrieves a string value for the preferences. The second parameter
		// is the default value to use if a preference value is not found.
		setsPref(sharedPrefs.getString("listPref", "Wi-Fi"));

		updateConnectionStatus();

		// Only loads the page if refreshDisplay is true. Otherwise, keeps
		// previous
		// display. For example, if the user has set "Wi-Fi only" in prefs and
		// the
		// device loses its Wi-Fi connection midway through the user using the
		// app,
		// you don't want to refresh the display--this would force the display
		// of
		// an error page instead of newsycombinator.com content.
		if (isRefreshDisplay()) {
			loadRss();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (receiver != null) {
			this.unregisterReceiver(receiver);
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Log.d(APP_TAG, getResources().getString(R.string.selected_item) + id);

	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		super.onMenuItemSelected(featureId, item);
		this.loadRss();
		return true;
	}

	/*
	 * Checks the network connection and sets the wifiConnected and
	 * mobileConnected variables accordingly
	 */
	private void updateConnectionStatus() {
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
		if (activeInfo != null && activeInfo.isConnected()) {
			// the connectivity with wifi was established
			wifiConnected = activeInfo.getType() == ConnectivityManager.TYPE_WIFI;
			mobileConnected = activeInfo.getType() == ConnectivityManager.TYPE_MOBILE;
		} else {
			// connectivity of the device is false
			wifiConnected = false;
			mobileConnected = false;
		}
		Log.d(APP_TAG, getResources().getString(R.string.via_wifi)
				+ wifiConnected);
		Log.d(APP_TAG, getResources().getString(R.string.via_mobile)
				+ mobileConnected);

	}

	// Uses AsyncTask subclass to download the XML feed from stackoverflow.com.
	// This avoids UI lock up. To prevent network operations from
	// causing a delay that results in a poor user experience, always perform
	// network operations on a separate thread from the UI.
	private void loadRss() {
		if (((getsPref().equals(ANY)) && (wifiConnected || mobileConnected))
				|| ((getsPref().equals(WIFI)) && (wifiConnected))) {
			// AsyncTask subclass
			new DownloadXmlTask().execute(URL);
		} else {
			Toast.makeText(RssReaderApp.this,
					getResources().getString(R.string.connection_error),
					Toast.LENGTH_LONG).show();
		}
	}

	// Populates the activity's options menu.
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mainmenu, menu);
		return true;
	}

	// Handles the user's menu selection.
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.settings:
			Intent settingsActivity = new Intent(getBaseContext(),
					SettingsActivity.class);
			startActivity(settingsActivity);
			return true;
		case R.id.refresh:
			loadRss();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/*
	 * returns the preferences of the app instance
	 */
	public static String getsPref() {
		return sPref;
	}

	public static void setsPref(String sPref) {
		RssReaderApp.sPref = sPref;
	}

	public static boolean isRefreshDisplay() {
		return refreshDisplay;
	}

	public static void setRefreshDisplay(boolean refreshDisplay) {
		RssReaderApp.refreshDisplay = refreshDisplay;
	}

	/**
	 * Implementation of AsyncTask used to download XML feed from
	 * stackoverflow.com
	 */
	private class DownloadXmlTask extends AsyncTask<String, Void, List<Entry>> {
		private static final String DEBUG_TAG = "DownloadXmlTask";

		@Override
		protected List<Entry> doInBackground(String... urls) {
			try {
				return loadXmlFromNetwork(urls[0]);
			} catch (IOException e) {
				return exceptionAsEntryList(e,
						getResources().getString(R.string.connection_error));
			} catch (XmlPullParserException e) {
				return exceptionAsEntryList(e,
						getResources().getString(R.string.xml_error));
			}
		}

		private List<Entry> exceptionAsEntryList(Exception e,
				String exceptionMessage) {
			Entry entryException = new Entry();
			entryException.setTitle(exceptionMessage);
			List<Entry> exceptionList = new ArrayList<Entry>();
			exceptionList.add(entryException);
			Log.e("DownloadXmlTask Exception", e.toString());
			return exceptionList;
		}

		@Override
		protected void onPostExecute(List<Entry> result) {

			setListAdapter(new ListAdapter(RssReaderApp.this, R.layout.row,
					result));

			Toast.makeText(RssReaderApp.this,
					getResources().getString(R.string.loading_message),
					Toast.LENGTH_SHORT).show();

		}

		// Uploads XML from stackoverflow.com, parses it, and combines it with
		// HTML markup. Returns HTML string.
		private List<Entry> loadXmlFromNetwork(String urlString)
				throws XmlPullParserException, IOException {
			InputStream stream = null;
			RssXmlPullParser newscombinatorXmlParser = new RssXmlPullParser();
			List<Entry> entries = null;
			try {
				stream = downloadUrl(urlString);
				entries = newscombinatorXmlParser.parse(stream);
				Log.d(DEBUG_TAG, getResources().getString(R.string.stream_closed_debug));
				// Makes sure that the InputStream is closed after the app is
				// finished using it.
			} finally {
				if (stream != null) {
					stream.close();
					Log.d(DEBUG_TAG, getResources().getString(R.string.stream_closed));
				}
			}

			return entries;
		}

		// Given a string representation of a URL, sets up a connection and gets
		// an input stream.
		// Data for the user into the content from the transaction
		// Content for the content into the
		private InputStream downloadUrl(String urlString) throws IOException {
			URL url = new URL(urlString);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setReadTimeout(10000 /* milliseconds */);
			conn.setConnectTimeout(15000 /* milliseconds */);
			conn.setRequestMethod("GET");
			conn.setDoInput(true);
			// Starts the query
			conn.connect();
			Log.d(DEBUG_TAG, getResources().getString(R.string.query_started));
			return conn.getInputStream();
		}

	}

	private class ListAdapter extends ArrayAdapter<Entry> {
		private List<Entry> items;
		private int[] colors = new int[] { Color.BLACK, Color.DKGRAY };
		private int[] textColors = new int[] { Color.GRAY, Color.LTGRAY };

		public ListAdapter(Context context, int textViewResourceId,
				List<Entry> items) {
			super(context, textViewResourceId, items);
			this.items = items;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.row, null);
			}
			final Entry item = items.get(position);
			if (item != null) {
				TextView tView = (TextView) v.findViewById(R.id.rss_entry_row);
				tView.setMovementMethod(ScrollingMovementMethod.getInstance());
				if (tView != null) {
					// Setting the title of the TextView
					tView.setText(item.getTitle());
					// Setting the link on clickable item
					tView.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View v) {
							Log.d(LISTENER_TAG, getResources().getString(R.string.url_detail)
									+ item.getLink().toString());
							try {
								// Start the activity
								Intent i = new Intent(Intent.ACTION_VIEW);
								i.setData(Uri.parse(item.getLink().toString()));
								startActivity(i);
							} catch (ActivityNotFoundException e) {
								// Raise on activity not found
								Toast.makeText(RssReaderApp.this,
										getResources().getString(R.string.browser_not_found),
										Toast.LENGTH_SHORT);
								Log.e(LISTENER_TAG, e.toString());
							}
						}
					});
					//Alternate Row Color
					int colorPos = position % colors.length;
					tView.setBackgroundColor(colors[colorPos]);
					tView.setTextColor(textColors[colorPos]);
				}

			}

			return v;
		}
	}

}