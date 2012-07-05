package com.example.android.networkusage;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Xml;

/**
 * Parse the newsYcombinator rss based on the requested feed Uses the
 * XmlPullParser for android skiping the unneeded(s) tags
 * 
 * @author abenedetti
 */
public class NewsYCombinatorParser {
	/*
	 * no namespace
	 */
	private static final String ns = null;
	private static final String DEBUG_TAG = "NewsYCombinatorParser";
	private boolean done;

	public List<Entry> parse(InputStream in) throws XmlPullParserException,
			IOException {
		try {
			XmlPullParser parser = Xml.newPullParser();
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(in, null);
			parser.nextTag();
			return processParent(parser);
		} finally {
			in.close();
		}
	}

	private List<Entry> processParent(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		List<Entry> entries = new ArrayList<Entry>();

		parser.require(XmlPullParser.START_TAG, ns, "rss");
		while (parser.next() != XmlPullParser.END_TAG ) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			// Starts by looking for the channel tag
			if (name.equals("channel")) {
				return processChannel(parser);
			} else {
				skip(parser);
			}
		}
		return entries;
	}

	private List<Entry> processChannel(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, ns, "channel");
		List<Entry> entries = new ArrayList<Entry>();
		while (parser.next() != XmlPullParser.END_TAG) {
			String name = parser.getName();
			// Starts by looking for the channel tag
			if (name.equalsIgnoreCase("item")) {
				entries.add(processItem(parser));
			} else {
                skip(parser);
            }
		}
		if (parser.getEventType() != XmlPullParser.START_TAG) {
			done = true; // Obsolete code the channel
		}
		return entries;
	}

	/*
	 * Send to the parser processor the instance from the parser
	 */
	private Entry processItem(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, ns, "item");
		String title = null;
		String description = null;
		String link = null;

		while (parser.next() != XmlPullParser.END_TAG) {
			if (!isEndTag(parser)) {
				String name = parser.getName();
				if (name.equalsIgnoreCase("title")) {
					title = extractTagContent(parser, "title");
				} else if (name.equalsIgnoreCase("link")) {
					link = extractTagContent(parser, "link");
				} else if (name.equalsIgnoreCase("description")) {
					description = extractTagContent(parser, "description");
				} else {
					skip(parser);
				}
			} else {
				continue;
			}
		}
		return new Entry(title, description, link);
	}

	private boolean isEndTag(XmlPullParser parser)
			throws XmlPullParserException {
		if (parser.getEventType() != XmlPullParser.START_TAG) {
			return true;
		}
		return false;
	}

	/**
	 * Obtains the content from the given tag
	 * 
	 * @param parser
	 * @param tag
	 * @return
	 * @throws XmlPullParserException
	 * @throws IOException
	 */
	private String extractTagContent(XmlPullParser parser, String tag)
			throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, ns, tag);
		String content = readText(parser);
		parser.require(XmlPullParser.END_TAG, ns, tag);
		return content;
	}

	/**
	 * For the tags title and description, extracts their text values.
	 * 
	 * @param parser
	 * @return
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	private String readText(XmlPullParser parser) throws IOException,
			XmlPullParserException {
		String result = "";
		if (parser.next() == XmlPullParser.TEXT) {
			result = parser.getText();
			parser.nextTag();
		}
		return result;
	}

    // Skips tags the parser isn't interested in. Uses depth to handle nested tags. i.e.,
    // if the next tag after a START_TAG isn't a matching END_TAG, it keeps going until it
    // finds the matching END_TAG (as indicated by the value of "depth" being 0).
    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

	/**
	 * This class represents a single entry (post) in the XML feed. It includes
	 * the data members "title," "link," and "description."
	 */
	public static class Entry {
		public final String title;
		public final String link;
		public final String description;

		private Entry(String title, String description, String link) {
			this.title = title;
			this.description = description;
			this.link = link;
		}
	}

}
