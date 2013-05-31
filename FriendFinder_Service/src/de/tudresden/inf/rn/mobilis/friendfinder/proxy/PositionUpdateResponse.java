package de.tudresden.inf.rn.mobilis.friendfinder.proxy;

import org.xmlpull.v1.XmlPullParser;
import de.tudresden.inf.rn.mobilis.xmpp.beans.XMPPBean;

import java.util.List;import java.util.ArrayList;

public class PositionUpdateResponse extends XMPPBean {

	private ClientDataList clientData = new ClientDataList();


	public PositionUpdateResponse( ClientDataList clientData ) {
		super();
		this.clientData = clientData;

		this.setType( XMPPBean.TYPE_RESULT );
	}

	public PositionUpdateResponse(){
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
				else if (tagName.equals( ClientDataList.CHILD_ELEMENT ) ) {
					this.clientData.fromXML( parser );
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

	public static final String CHILD_ELEMENT = "PositionUpdateResponse";

	@Override
	public String getChildElement() {
		return CHILD_ELEMENT;
	}

	public static final String NAMESPACE = "eet:iq:position_update";

	@Override
	public String getNamespace() {
		return NAMESPACE;
	}

	@Override
	public XMPPBean clone() {
		PositionUpdateResponse clone = new PositionUpdateResponse( clientData );
		clone.cloneBasicAttributes( clone );

		return clone;
	}

	@Override
	public String payloadToXML() {
		StringBuilder sb = new StringBuilder();

		sb.append( "<" + this.clientData.getChildElement() + ">" )
			.append( this.clientData.toXML() )
			.append( "</" + this.clientData.getChildElement() + ">" );

		sb = appendErrorPayload(sb);

		return sb.toString();
	}


	public ClientDataList getClientData() {
		return this.clientData;
	}

	public void setClientData( ClientDataList clientData ) {
		this.clientData = clientData;
	}

}