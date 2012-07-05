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

package com.example.android.networkusage;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;

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
public class NetworkActivity extends Activity {
    public static final String WIFI = "Wi-Fi";
    public static final String ANY = "Any";
    private static final String URL = "http://news.ycombinator.com/rss";
    private static final String DEBUG_TAG = "NetworkActivity debug ";

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
        // an error page instead of stackoverflow.com content.
        if (isRefreshDisplay()) {
            loadPage();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (receiver != null) {
            this.unregisterReceiver(receiver);
        }
    }

    /*
      * Checks the network connection and sets the wifiConnected and mobileConnected variables accordingly
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
        Log.d(DEBUG_TAG, "Wifi connected: " + wifiConnected);
        Log.d(DEBUG_TAG, "Mobile connected: " + mobileConnected);

    }

    // Uses AsyncTask subclass to download the XML feed from stackoverflow.com.
    // This avoids UI lock up. To prevent network operations from
    // causing a delay that results in a poor user experience, always perform
    // network operations on a separate thread from the UI.
    private void loadPage() {
        if (((getsPref().equals(ANY)) && (wifiConnected || mobileConnected))
                || ((getsPref().equals(WIFI)) && (wifiConnected))) {
            // AsyncTask subclass
            new DownloadXmlTask(this).execute(URL);
        } else {
            showErrorPage();
        }
    }

    // Displays an error if the app is unable to load content.
    private void showErrorPage() {
        setContentView(R.layout.main);

        // The specified network connection is not available. Displays error
        // message.
        WebView myWebView = (WebView) findViewById(R.id.webview);
        myWebView.loadData(getResources().getString(R.string.connection_error),
                "text/html", null);
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
                loadPage();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static String getsPref() {
        return sPref;
    }

    public static void setsPref(String sPref) {
        NetworkActivity.sPref = sPref;
    }

    public static boolean isRefreshDisplay() {
        return refreshDisplay;
    }

    public static void setRefreshDisplay(boolean refreshDisplay) {
        NetworkActivity.refreshDisplay = refreshDisplay;
    }

}
