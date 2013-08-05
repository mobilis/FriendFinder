package de.tudresden.inf.rn.mobilis.friendfinder.proxy;

import de.tudresden.inf.rn.mobilis.xmpp.beans.XMPPInfo;import de.tudresden.inf.rn.mobilis.xmpp.beans.XMPPBean;import org.xmlpull.v1.XmlPullParser;import java.util.List;import java.util.ArrayList;

public class MUCMessage implements XMPPInfo {

	private String jid = null;
	private String text = null;
	private ClientData clientData = new ClientData();


	public MUCMessage( String jid, String text, ClientData clientData ) {
		super();
		this.jid = jid;
		this.text = text;
		this.clientData = clientData;
	}

	public MUCMessage(){}



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
				else if (tagName.equals( "text" ) ) {
					this.text = parser.nextText();
				}
				else if (tagName.equals( ClientData.CHILD_ELEMENT ) ) {
					this.clientData.fromXML( parser );
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

	public static final String CHILD_ELEMENT = "MUCMessage";

	@Override
	public String getChildElement() {
		return CHILD_ELEMENT;
	}

	public static final String NAMESPACE = "http://mobilis.inf.tu-dresden.de#services/FriendFinder#type:MUCMessage";

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

		sb.append( "<text>" )
			.append( this.text )
			.append( "</text>" );

		sb.append( "<" + this.clientData.getChildElement() + ">" )
			.append( this.clientData.toXML() )
			.append( "</" + this.clientData.getChildElement() + ">" );

		return sb.toString();
	}



	public String getJid() {
		return this.jid;
	}

	public void setJid( String jid ) {
		this.jid = jid;
	}

	public String getText() {
		return this.text;
	}

	public void setText( String text ) {
		this.text = text;
	}

	public ClientData getClientData() {
		return this.clientData;
	}

	public void setClientData( ClientData clientData ) {
		this.clientData = clientData;
	}

}