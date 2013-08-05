package de.tudresden.inf.rn.mobilis.friendfinder.clientstub;

import de.tudresden.inf.rn.mobilis.xmpp.beans.XMPPInfo;import de.tudresden.inf.rn.mobilis.xmpp.beans.XMPPBean;import org.xmlpull.v1.XmlPullParser;import java.util.List;import java.util.ArrayList;

public class IsTrackAvailableRequest extends XMPPBean {

	private ClientLocation position = new ClientLocation();
	private int trackId = Integer.MIN_VALUE;


	public IsTrackAvailableRequest( ClientLocation position, int trackId ) {
		super();
		this.position = position;
		this.trackId = trackId;

		this.setType( XMPPBean.TYPE_SET );
	}

	public IsTrackAvailableRequest(){
		this.setType( XMPPBean.TYPE_SET );
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
				else if (tagName.equals( ClientLocation.CHILD_ELEMENT ) ) {
					this.position.fromXML( parser );
				}
				else if (tagName.equals( "trackId" ) ) {
					this.trackId = Integer.parseInt( parser.nextText() );
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

	public static final String CHILD_ELEMENT = "IsTrackAvailableRequest";

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
		IsTrackAvailableRequest clone = new IsTrackAvailableRequest( position, trackId );
		clone.cloneBasicAttributes( clone );

		return clone;
	}

	@Override
	public String payloadToXML() {
		StringBuilder sb = new StringBuilder();

		sb.append( "<" + this.position.getChildElement() + ">" )
			.append( this.position.toXML() )
			.append( "</" + this.position.getChildElement() + ">" );

		sb.append( "<trackId>" )
			.append( this.trackId )
			.append( "</trackId>" );

		sb = appendErrorPayload(sb);

		return sb.toString();
	}


	public ClientLocation getPosition() {
		return this.position;
	}

	public void setPosition( ClientLocation position ) {
		this.position = position;
	}

	public int getTrackId() {
		return this.trackId;
	}

	public void setTrackId( int trackId ) {
		this.trackId = trackId;
	}

}