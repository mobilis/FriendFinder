package de.tudresden.inf.rn.mobilis.friendfinder.clientstub;

import org.xmlpull.v1.XmlPullParser;
import de.tudresden.inf.rn.mobilis.xmpp.beans.XMPPInfo;

import java.util.List;import java.util.ArrayList;

public class ClientLocation implements XMPPInfo {

	private double Lat = Double.MIN_VALUE;
	private double Lng = Double.MIN_VALUE;
	private double Accuracy = Double.MIN_VALUE;


	public ClientLocation( double Lat, double Lng, double Accuracy ) {
		super();
		this.Lat = Lat;
		this.Lng = Lng;
		this.Accuracy = Accuracy;
	}

	public ClientLocation(){}



	@Override
	public void fromXML( XmlPullParser parser ) throws Exception {
		boolean done = false;
			
		do {
			switch (parser.getEventType()) {
			case XmlPullParser.START_TAG:
				String tagName = parser.getName();
				
				if (tagName.equals(getChildElement())) {
					parser.next();
				}
				else if (tagName.equals( "Lat" ) ) {
					this.Lat = Double.parseDouble( parser.nextText() );
				}
				else if (tagName.equals( "Lng" ) ) {
					this.Lng = Double.parseDouble( parser.nextText() );
				}
				else if (tagName.equals( "Accuracy" ) ) {
					this.Accuracy = Double.parseDouble( parser.nextText() );
				}
				else
					parser.next();
				break;
			case XmlPullParser.END_TAG:
				if (parser.getName().equals(getChildElement()))
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

	public static final String CHILD_ELEMENT = "ClientLocation";

	@Override
	public String getChildElement() {
		return CHILD_ELEMENT;
	}

	public static final String NAMESPACE = "http://joyo.diskstation.org#services/FriendFinder#type:ClientLocation";

	@Override
	public String getNamespace() {
		return NAMESPACE;
	}

	@Override
	public String toXML() {
		StringBuilder sb = new StringBuilder();

		sb.append( "<Lat>" )
			.append( this.Lat )
			.append( "</Lat>" );

		sb.append( "<Lng>" )
			.append( this.Lng )
			.append( "</Lng>" );

		sb.append( "<Accuracy>" )
			.append( this.Accuracy )
			.append( "</Accuracy>" );

		return sb.toString();
	}



	public double getLat() {
		return this.Lat;
	}

	public void setLat( double Lat ) {
		this.Lat = Lat;
	}

	public double getLng() {
		return this.Lng;
	}

	public void setLng( double Lng ) {
		this.Lng = Lng;
	}

	public double getAccuracy() {
		return this.Accuracy;
	}

	public void setAccuracy( double Accuracy ) {
		this.Accuracy = Accuracy;
	}

}