/*
 * Copyright (C) 2013
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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;

import android.util.Log;
import android.util.Xml;

/**
 * Parses the rss feeds using XmlPullParser for android
 *
 * @author Adolfo Benedetti
 */
public class RssXmlPullParser {
    // names of the XML tags
    static final String CHANNEL = "channel";
    static final String DESCRIPTION = "description";
    static final String LINK = "link";
    static final String TITLE = "title";
    static final String ITEM = "item";

    public List<Entry> parse(InputStream in) {
        List<Entry> messages = null;
        XmlPullParser parser = Xml.newPullParser();
        try {
            // auto-detect the encoding from the stream
            parser.setInput(in, null);
            int eventType = parser.getEventType();
            Entry currentEntry = null;
            boolean done = false;
            while (eventType != XmlPullParser.END_DOCUMENT && !done) {
                String name;
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        messages = new ArrayList<Entry>();
                        break;
                    case XmlPullParser.START_TAG:
                        name = parser.getName();
                        if (name.equalsIgnoreCase(ITEM)) {
                            currentEntry = new Entry();
                        } else if (currentEntry != null) {
                            if (name.equalsIgnoreCase(LINK)) {
                                currentEntry.setLink(parser.nextText());
                                //saving time without processing description
                            } else if (name.equalsIgnoreCase(DESCRIPTION) && RssReaderApp.isWithDescription()) {
                                currentEntry.setDescription(parser.nextText());
                            } else if (name.equalsIgnoreCase(TITLE)) {
                                currentEntry.setTitle(parser.nextText());
                            }
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        name = parser.getName();
                        if (name.equalsIgnoreCase(ITEM) && currentEntry != null) {
                            if (messages != null) {
                                messages.add(currentEntry);
                            }
                        } else if (name.equalsIgnoreCase(CHANNEL)) {
                            done = true;
                        }
                        break;
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            Log.e("RssREaderParser::PullFeedParser", e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return messages;
    }

}
