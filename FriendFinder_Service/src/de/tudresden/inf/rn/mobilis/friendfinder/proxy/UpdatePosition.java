package de.tudresden.inf.rn.mobilis.friendfinder.proxy;

import org.xmlpull.v1.XmlPullParser;
import de.tudresden.inf.rn.mobilis.xmpp.beans.XMPPInfo;

import java.util.List;import java.util.ArrayList;

public class UpdatePosition implements XMPPInfo {

	private String jid = null;
	private ClientLocation location = new ClientLocation();


	public UpdatePosition( String jid, ClientLocation location ) {
		super();
		this.jid = jid;
		this.location = location;
	}

	public UpdatePosition(){}



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
				else if (tagName.equals( "jid" ) ) {
					this.jid = parser.nextText();
				}
				else if (tagName.equals( ClientLocation.CHILD_ELEMENT ) ) {
					this.location.fromXML( parser );
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

	public static final String CHILD_ELEMENT = "UpdatePosition";

	@Override
	public String getChildElement() {
		return CHILD_ELEMENT;
	}

	public static final String NAMESPACE = "http://joyo.diskstation.org#services/FriendFinder#type:UpdatePosition";

	@Override
	public String getNamespace() {
		return NAMESPACE;
	}

	@Override
	public String toXML() {
		StringBuilder sb = new StringBuilder();

		sb.append( "<jid>" )
			.append( this.jid )
			.append( "</jid>" );

		sb.append( "<" + this.location.getChildElement() + ">" )
			.append( this.location.toXML() )
			.append( "</" + this.location.getChildElement() + ">" );

		return sb.toString();
	}



	public String getJid() {
		return this.jid;
	}

	public void setJid( String jid ) {
		this.jid = jid;
	}

	public ClientLocation getLocation() {
		return this.location;
	}

	public void setLocation( ClientLocation location ) {
		this.location = location;
	}

}