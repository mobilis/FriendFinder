package de.tudresden.inf.rn.mobilis.friendfinder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

public class GPXTrack {
	private final static Logger LOG = Logger.getLogger(DBProxy.class
			.getCanonicalName());
	
	protected File mGpxFile;

	// metadata
	protected long time;

	// trk
	protected String trkName;
	protected ArrayList<Trkpt> trkpt;

	// xml
	protected final String XML_GPX = "gpx";
	protected final String XML_TIME = "time";
	protected final String XML_META = "metadata";
	protected final String XML_TRK = "trk";
	protected final String XML_NAME = "name";
	protected final String XML_TRKSEG = "trkseg";
	
	public static final String CHILD_ELEMENT = "gpxTrack";

	protected final String mDateFormatString = "yyyy-MM-dd'T'HH:mm:ss'Z'";

	public GPXTrack() {
		trkpt = new ArrayList<Trkpt>();
	}
	
	@Override
	public GPXTrack clone(){
		GPXTrack gpx = this.clone();
		gpx.trkpt = (ArrayList<Trkpt>) this.trkpt.clone();
		return gpx;
	}

	public void addTrackPoint(double lat, double lon, String activity,
			float speed, long time, float ele, float acc) {
		trkpt.add(new Trkpt(lat, lon, activity, speed, time, ele, acc));
	}

	public void addTrackPoint(double lat, double lon, String activity,
			float speed) {
		trkpt.add(new Trkpt(lat, lon, activity, speed));
	}

	public String toXML(){ return this.toXML(true); }
	
	public String toXML(Boolean forIQ) {
		final String START = "<";
		final String END = ">";
		String NEWLINE = "\n";
		String NEWTAB = "\t";
		if(forIQ){
			NEWLINE = "";
			NEWTAB = "";
		}	
		
		DateFormat mDateFormatter = new SimpleDateFormat(mDateFormatString,
				Locale.GERMANY);

		StringBuilder sb = new StringBuilder();
		if(!forIQ) 
			sb.append(START + "?xml version=\"1.0\" encoding=\"UTF-8\" ?" + END + NEWLINE);
		sb.append(START + "gpx xmlns=\"http://www.topografix.com/GPX/1/1\"" + END + NEWLINE);

		sb.append(START + XML_META + END + NEWLINE + NEWTAB + START + XML_TIME + END
				+ mDateFormatter.format(time) + START + "/" + XML_TIME + END + NEWLINE + START + "/"
				+ XML_META + END + NEWLINE);
		sb.append(this.trackToXML(forIQ));

		sb.append(NEWLINE + START + "/gpx" + END);
		return sb.toString();
	}
	
	public String trackToXML(boolean forIQ){
		final String START = "<";
		final String END = ">";
		String NEWLINE = "\n";
		String NEWTAB = "\t";
		if(forIQ){
			NEWLINE = "";
			NEWTAB = "";
		}	
		
		DateFormat mDateFormatter = new SimpleDateFormat(mDateFormatString,
				Locale.GERMANY);

		StringBuilder sb = new StringBuilder();
		sb.append(START +  XML_TRK + END + NEWLINE + START + XML_NAME + END + trkName + START + "/"
				+ XML_NAME + END + NEWLINE + START + XML_TRKSEG + END + NEWLINE);

		for (Trkpt tp : trkpt) {
			sb.append(tp.toXML(mDateFormatter, forIQ));
		}

		sb.append(START + "/" + XML_TRKSEG + END + NEWLINE + START + "/" + XML_TRK + END);
		return sb.toString();
	}

	public void fromXML(String gpx){
		try {
			XmlPullParser xpp;
			xpp = XmlPullParserFactory.newInstance().newPullParser();
			xpp.setInput(new StringReader(gpx));
			this.fromXML(xpp);
		} catch (Exception e) {
			LOG.severe("!EXCEPTION " + e.toString());
		}
	}
	
	public void fromXML(XmlPullParser parser) throws Exception {
		boolean done = false;
		DateFormat mDateFormatter = new SimpleDateFormat(mDateFormatString,
				Locale.GERMANY);

		do {
			switch (parser.getEventType()) {
			case XmlPullParser.START_TAG:
				String tagName = parser.getName();

				if (tagName.equals(XML_GPX)) {
					parser.next();
				} else if (tagName.equals(Trkpt.XML_TRKPT)) {
					Trkpt trkpt = new Trkpt();
					trkpt.fromXML(parser, mDateFormatter);
					this.trkpt.add(trkpt);
				} else if (tagName.equals(XML_NAME)) {
					this.trkName = parser.nextText();
				} else if (tagName.equals(XML_TIME)) {
					try {
						Date date = mDateFormatter.parse(parser.nextText());
						this.time = date.getTime();
					} catch (ParseException e) {
						this.time = 0;
					}
				} else
					parser.next();
				break;
			case XmlPullParser.END_TAG:
				if (parser.getName().equals(XML_GPX))
					done = true;
				else
					parser.next();
				break;
			case XmlPullParser.END_DOCUMENT:
				done = true;
				break;
			default:
				parser.next();
			}
		} while (!done);
	}
	
	public ArrayList<Trkpt> getTrackPoints(){ return this.trkpt; }
	
	public String getChildElement(){ return GPXTrack.CHILD_ELEMENT; }

	/************** inner classes **********************/

	public class Trkpt {
		public double lat;
		public double lon;
		public double ele;
		public long time;
		public String activity;
		public float speed;
		public float accuracy;

		public static final String XML_TRKPT = "trkpt";
		protected final String XML_LAT = "lat";
		protected final String XML_LON = "lon";
		protected final String XML_ELE = "ele";
		protected final String XML_TIME = "time";
		protected final String XML_ACT = "act";
		protected final String XML_SPEED = "spd";
		protected final String XML_ACCURACY = "acc";

		public Trkpt() {
			this.lat = 0;
			this.lon = 0;
			this.activity = "unknown";
			this.speed = 0;
			this.time = 0;
			this.ele = 0;
		}

		public Trkpt(double lat, double lon, String activity, float speed,
				long time, float ele, float acc) {
			this.lat = lat;
			this.lon = lon;
			this.activity = activity;
			this.speed = speed;
			this.time = time;
			this.ele = ele;
			this.accuracy = acc;
		}

		public Trkpt(double lat, double lon, String activity, float speed) {
			this.lat = lat;
			this.lon = lon;
			this.activity = activity;
			this.speed = speed;
			this.time = System.currentTimeMillis();
			this.ele = 0;
			this.accuracy = 0;
		}

		public String toXML(DateFormat mDateFormatter, Boolean forIQ) {
			final String START = "<";
			final String END = ">";
			String NEWLINE = "\n";
			String NEWTAB = "\t";
			if(forIQ){
				NEWLINE = "";
				NEWTAB = "";
			}
			
			String trkpt = START + XML_TRKPT + " " + XML_LAT + "=\"" + lat
					+ "\" " + XML_LON + "=\"" + lon + "\"" + END + NEWLINE;
			if (ele != 0)
				trkpt += NEWTAB + START + XML_ELE + END + ele + START + "/" + XML_ELE + END + NEWLINE;
			if (time != 0)
				trkpt += NEWTAB + START + XML_TIME + END + mDateFormatter.format(time)
						+ START + "/" + XML_TIME + END + NEWLINE;
			trkpt += NEWTAB + START + "extention" + END + START + XML_ACT + END + activity + START + "/"
					+ XML_ACT + END + START + XML_SPEED + END + speed + START + "/"
					+ XML_SPEED + END + START + XML_ACCURACY + END + accuracy + START + "/" + XML_ACCURACY + END + START + "/extention" + END + NEWLINE;
			trkpt += START + "/" + XML_TRKPT + END + NEWLINE;
			return trkpt;
		}

		public void fromXML(XmlPullParser parser, DateFormat mDateFormatter)
				throws Exception {
			boolean done = false;

			do {
				switch (parser.getEventType()) {
				case XmlPullParser.START_TAG:
					String tagName = parser.getName();

					if (tagName.equals(XML_TRKPT)) {
						String lat = parser.getAttributeValue(null, XML_LAT);
						String lon = parser.getAttributeValue(null, XML_LON);
						this.lat = Double
								.parseDouble((lat != null ? lat : "0"));
						this.lon = Double
								.parseDouble((lon != null ? lon : "0"));
						parser.next();
					} else if (tagName.equals(XML_ACT)) {
						this.activity = parser.nextText();
					} else if (tagName.equals(XML_SPEED)) {
						this.speed = Float.parseFloat(parser.nextText());
					} else if (tagName.equals(XML_TIME)) {
						try {
							Date date = mDateFormatter.parse(parser.nextText());
							this.time = date.getTime();
						} catch (ParseException e) {
							this.time = 0;
						}
					} else if (tagName.equals(XML_ELE)) {
						this.ele = Double.parseDouble(parser.nextText());
					} else if (tagName.equals(XML_ACCURACY)) {
						this.accuracy = Float.parseFloat(parser.nextText());
					} else
						parser.next();
					break;
				case XmlPullParser.END_TAG:
					if (parser.getName().equals(XML_TRKPT))
						done = true;
					else
						parser.next();
					break;
				case XmlPullParser.END_DOCUMENT:
					done = true;
					break;
				default:
					parser.next();
				}
			} while (!done);
		}
	};
}
