package de.tudresden.inf.rn.mobilis.friendfinder.proxy;

import de.tudresden.inf.rn.mobilis.friendfinder.GPXTrack;
import de.tudresden.inf.rn.mobilis.xmpp.beans.XMPPBean;import org.xmlpull.v1.XmlPullParser;import java.util.List;import java.util.ArrayList;

public class IsTrackAvailableResponse extends XMPPBean {

	private boolean result = false;
	private int trackId = Integer.MIN_VALUE;
	private GPXTrack gpxTrack = new GPXTrack();


	public IsTrackAvailableResponse( boolean result, int trackId, GPXTrack gpxTrack ) {
		super();
		this.result = result;
		this.trackId = trackId;
		this.gpxTrack = gpxTrack;

		this.setType( XMPPBean.TYPE_RESULT );
	}

	public IsTrackAvailableResponse(){
		this.setType( XMPPBean.TYPE_RESULT );
	}


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
				else if (tagName.equals( "result" ) ) {
					this.result = Boolean.parseBoolean( parser.nextText() );
				}
				else if (tagName.equals( "trackId" ) ) {
					this.trackId = Integer.parseInt( parser.nextText() );
				}
				else if (tagName.equals( GPXTrack.CHILD_ELEMENT ) ) {
					this.gpxTrack.fromXML( parser );
				}
				else if (tagName.equals("error")) {
					parser = parseErrorAttributes(parser);
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

	public static final String CHILD_ELEMENT = "IsTrackAvailableResponse";

	@Override
	public String getChildElement() {
		return CHILD_ELEMENT;
	}

	public static final String NAMESPACE = "eet:iq:is_track_available";

	@Override
	public String getNamespace() {
		return NAMESPACE;
	}

	@Override
	public XMPPBean clone() {
		IsTrackAvailableResponse clone = new IsTrackAvailableResponse( result, trackId, gpxTrack );
		clone.cloneBasicAttributes( clone );

		return clone;
	}

	@Override
	public String payloadToXML() {
		StringBuilder sb = new StringBuilder();

		sb.append( "<result>" )
			.append( this.result )
			.append( "</result>" );

		sb.append( "<trackId>" )
			.append( this.trackId )
			.append( "</trackId>" );

		sb.append( "<" + this.gpxTrack.getChildElement() + ">" )
			.append( this.gpxTrack.toXML() )
			.append( "</" + this.gpxTrack.getChildElement() + ">" );

		sb = appendErrorPayload(sb);

		return sb.toString();
	}


	public boolean getResult() {
		return this.result;
	}

	public void setResult( boolean result ) {
		this.result = result;
	}

	public int getTrackId() {
		return this.trackId;
	}

	public void setTrackId( int trackId ) {
		this.trackId = trackId;
	}

	public GPXTrack getGpxTrack() {
		return this.gpxTrack;
	}

	public void setGpxTrack( GPXTrack gpxTrack ) {
		this.gpxTrack = gpxTrack;
	}

}