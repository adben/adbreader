README
======

Adb Rss Reader.
This application does the following:
-- Presents TextViews that has a list of HTML links to the latest items from the selected rss feed
-- Parses the rss feed using XMLPullParser.
-- Uses AsyncTask to download and process the XML feed. 
-- Monitors preferences and the device's network connection to determine whether to refresh the TextView content.
