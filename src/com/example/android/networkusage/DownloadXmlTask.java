package com.example.android.networkusage;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.WebView;

import com.example.android.networkusage.StackOverflowXmlParser.Entry;

/**
 * Implementation of AsyncTask used to download XML feed from stackoverflow.com
 * 
 * @author abenedetti
 * 
 */
public class DownloadXmlTask extends AsyncTask<String, Void, String> {
	private static final String DEBUG_TAG = "DownloadXmlTask";
	NetworkActivity mNetworkActivity;

	public DownloadXmlTask(NetworkActivity networkActivity) {
		mNetworkActivity = networkActivity;
		Log.d(DEBUG_TAG, "Asynchronous download xml task started !");
	}

	@Override
	protected String doInBackground(String... urls) {
		try {
			return loadXmlFromNetwork(urls[0]);
		} catch (IOException e) {
			return mNetworkActivity.getResources().getString(
					R.string.connection_error);
		} catch (XmlPullParserException e) {
			return mNetworkActivity.getResources()
					.getString(R.string.xml_error);
		}
	}

	@Override
	protected void onPostExecute(String result) {
		mNetworkActivity.setContentView(R.layout.main);
		// Displays the HTML string in the UI via a WebView
		WebView myWebView = (WebView) mNetworkActivity
				.findViewById(R.id.webview);
		myWebView.loadData(result, "text/html", null);
		Log.d(DEBUG_TAG, "Displayed the HTML string into the UI via WebView");
	}

	// Uploads XML from stackoverflow.com, parses it, and combines it with
	// HTML markup. Returns HTML string.
	private String loadXmlFromNetwork(String urlString)
			throws XmlPullParserException, IOException {
		InputStream stream = null;
		StackOverflowXmlParser stackOverflowXmlParser = new StackOverflowXmlParser();
		List<Entry> entries = null;
		Calendar rightNow = Calendar.getInstance();
		DateFormat formatter = new SimpleDateFormat("MMM dd h:mmaa");

		// Checks whether the user set the preference to include summary text
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(mNetworkActivity);
		boolean pref = sharedPrefs.getBoolean("summaryPref", false);

		StringBuilder htmlString = new StringBuilder();
		htmlString.append("<h3>"
				+ mNetworkActivity.getResources()
						.getString(R.string.page_title) + "</h3>");
		htmlString.append("<em>"
				+ mNetworkActivity.getResources().getString(R.string.updated)
				+ " " + formatter.format(rightNow.getTime()) + "</em>");
		Log.d(DEBUG_TAG, "Html String displayed " + htmlString.toString());

		try {
			stream = downloadUrl(urlString);
			entries = stackOverflowXmlParser.parse(stream);
			Log.d(DEBUG_TAG,
					"InputStream to closed : needed after the app is finished using it");
			// Makes sure that the InputStream is closed after the app is
			// finished using it.
		} finally {
			if (stream != null) {
				stream.close();
				Log.d(DEBUG_TAG, "InputStream is now closed!");
			}
		}

		// StackOverflowXmlParser returns a List (called "entries") of Entry
		// objects.
		// Each Entry object represents a single post in the XML feed.
		// This section processes the entries list to combine each entry with
		// HTML markup.
		// Each entry is displayed in the UI as a link that optionally includes
		// a text summary.
		for (Entry entry : entries) {
			htmlString.append("<p><a href='");
			htmlString.append(entry.link);
			htmlString.append("'>" + entry.title + "</a></p>");
			// If the user set the preference to include summary text,
			// adds it to the display.
			if (pref) {
				htmlString.append(entry.summary);
			}
			Log.d(DEBUG_TAG, "Entry processed : " + entry.title);
		}
		return htmlString.toString();
	}

	// Given a string representation of a URL, sets up a connection and gets
	// an input stream.
	private InputStream downloadUrl(String urlString) throws IOException {
		URL url = new URL(urlString);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setReadTimeout(10000 /* milliseconds */);
		conn.setConnectTimeout(15000 /* milliseconds */);
		conn.setRequestMethod("GET");
		conn.setDoInput(true);
		// Starts the query
		conn.connect();
		Log.d(DEBUG_TAG, "Starts the query");
		InputStream stream = conn.getInputStream();
		return stream;
	}

}
