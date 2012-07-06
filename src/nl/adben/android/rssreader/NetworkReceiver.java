package nl.adben.android.rssreader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

/**
 * This BroadcastReceiver intercepts the
 * android.net.ConnectivityManager.CONNECTIVITY_ACTION, which indicates a
 * connection change. It checks whether the type is TYPE_WIFI. If it is, it
 * checks whether Wi-Fi is connected and sets the wifiConnected flag in the main
 * activity accordingly.
 */
public class NetworkReceiver extends BroadcastReceiver {
	RssReaderApp mNetworkActivity;
	private static final String DEBUG_TAG = "NetworkReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		ConnectivityManager connMgr = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

		// Checks the user prefs and the network connection. Based on the
		// result, decides
		// whether
		// to refresh the display or keep the current display.
		// If the userpref is Wi-Fi only, checks to see if the device has a
		// Wi-Fi connection.
		if (RssReaderApp.WIFI.equals(RssReaderApp.getsPref())
				&& networkInfo != null
				&& networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
			// If device has its Wi-Fi connection, sets refreshDisplay
			// to true. This causes the display to be refreshed when the user
			// returns to the app.
			RssReaderApp.setRefreshDisplay(true);
			Toast.makeText(context, R.string.wifi_connected, Toast.LENGTH_SHORT)
					.show();
			Log.d(DEBUG_TAG,
					"Devices has its Wifi connnection, refeshDisplay => true");

			// If the setting is ANY network and there is a network connection
			// (which by process of elimination would be mobile), sets
			// refreshDisplay to true.
		} else if (RssReaderApp.ANY.equals(RssReaderApp.getsPref())
				&& networkInfo != null) {
			RssReaderApp.setRefreshDisplay(true);
			Log.d(DEBUG_TAG,
					"Devices has another connnection, refeshDisplay => true");

			// Otherwise, the app can't download content--either because there
			// is no network
			// connection (mobile or Wi-Fi), or because the pref setting is
			// WIFI, and there
			// is no Wi-Fi connection.
			// Sets refreshDisplay to false.
		} else {
			RssReaderApp.setRefreshDisplay(false);
			Toast.makeText(context, R.string.lost_connection,
					Toast.LENGTH_SHORT).show();
			Log.d(DEBUG_TAG,
					"Devices has another connnection, refeshDisplay => false");
		}
	}

}
