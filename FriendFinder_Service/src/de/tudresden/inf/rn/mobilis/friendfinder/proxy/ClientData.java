package de.tudresden.inf.rn.mobilis.friendfinder.proxy;

import org.xmlpull.v1.XmlPullParser;
import de.tudresden.inf.rn.mobilis.xmpp.beans.XMPPInfo;

import java.util.List;import java.util.ArrayList;

public class ClientData implements XMPPInfo {

	private String name = null;
	private String jid = null;
	private String color = null;
	private boolean positionUpdateEnabled = false;
	private ClientLocation clientLocation = new ClientLocation();


	public ClientData( String name, String jid, String color, boolean positionUpdateEnabled, ClientLocation clientLocation ) {
		super();
		this.name = name;
		this.jid = jid;
		this.color = color;
		this.positionUpdateEnabled = positionUpdateEnabled;
		this.clientLocation = clientLocation;
	}

	public ClientData(){}



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
				else if (tagName.equals( "name" ) ) {
					this.name = parser.nextText();
				}
				else if (tagName.equals( "jid" ) ) {
					this.jid = parser.nextText();
				}
				else if (tagName.equals( "color" ) ) {
					this.color = parser.nextText();
				}
				else if (tagName.equals( "positionUpdateEnabled" ) ) {
					this.positionUpdateEnabled = Boolean.parseBoolean( parser.nextText() );
				}
				else if (tagName.equals( ClientLocation.CHILD_ELEMENT ) ) {
					this.clientLocation.fromXML( parser );
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

	public static final String CHILD_ELEMENT = "ClientData";

	@Override
	public String getChildElement() {
		return CHILD_ELEMENT;
	}

	public static final String NAMESPACE = "http://joyo.diskstation.org#services/FriendFinder#type:ClientData";

	@Override
	public String getNamespace() {
		return NAMESPACE;
	}

	@Override
	public String toXML() {
		StringBuilder sb = new StringBuilder();

		sb.append( "<name>" )
			.append( this.name )
			.append( "</name>" );

		sb.append( "<jid>" )
			.append( this.jid )
			.append( "</jid>" );

		sb.append( "<color>" )
			.append( this.color )
			.append( "</color>" );

		sb.append( "<positionUpdateEnabled>" )
			.append( this.positionUpdateEnabled )
			.append( "</positionUpdateEnabled>" );

		sb.append( "<" + this.clientLocation.getChildElement() + ">" )
			.append( this.clientLocation.toXML() )
			.append( "</" + this.clientLocation.getChildElement() + ">" );

		return sb.toString();
	}



	public String getName() {
		return this.name;
	}

	public void setName( String name ) {
		this.name = name;
	}

	public String getJid() {
		return this.jid;
	}

	public void setJid( String jid ) {
		this.jid = jid;
	}

	public String getColor() {
		return this.color;
	}

	public void setColor( String color ) {
		this.color = color;
	}

	public boolean getPositionUpdateEnabled() {
		return this.positionUpdateEnabled;
	}

	public void setPositionUpdateEnabled( boolean positionUpdateEnabled ) {
		this.positionUpdateEnabled = positionUpdateEnabled;
	}

	public ClientLocation getClientLocation() {
		return this.clientLocation;
	}

	public void setClientLocation( ClientLocation clientLocation ) {
		this.clientLocation = clientLocation;
	}

}