package de.tudresden.inf.rn.mobilis.friendfinder.clientstub;

import org.xmlpull.v1.XmlPullParser;
import de.tudresden.inf.rn.mobilis.xmpp.beans.XMPPInfo;

import java.util.List;import java.util.ArrayList;

public class ClientDataList implements XMPPInfo {

	private ArrayList<ClientData> clientData;


	public ClientDataList( ArrayList<ClientData> clientData ) {
		super();
		this.clientData = clientData;
	}

	public ClientDataList(){
		this.clientData = new ArrayList<ClientData>();
	}



	@Override
	public void fromXML( XmlPullParser parser ) throws Exception {
		boolean done = false;
		this.clientData.clear();
			
		do {
			switch (parser.getEventType()) {
			case XmlPullParser.START_TAG:
				String tagName = parser.getName();
				
				if (tagName.equals(getChildElement())) {
					parser.next();
				}
				else if (tagName.equals( ClientData.CHILD_ELEMENT ) ) {
					ClientData cd = new ClientData();
					cd.fromXML( parser );
					this.clientData.add(cd);
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

	public static final String CHILD_ELEMENT = "ClientDataList";

	@Override
	public String getChildElement() {
		return CHILD_ELEMENT;
	}

	public static final String NAMESPACE = "http://joyo.diskstation.org#services/FriendFinder#type:ClientDataList";

	@Override
	public String getNamespace() {
		return NAMESPACE;
	}

	@Override
	public String toXML() {
		StringBuilder sb = new StringBuilder();

		for(ClientData cd: clientData){
			sb.append( "<" + cd.getChildElement() + ">" )
				.append( cd.toXML() )
				.append( "</" + cd.getChildElement() + ">" );
		}

		return sb.toString();
	}



	public ArrayList<ClientData> getClientData() {
		return this.clientData;
	}

	public void setClientData( ArrayList<ClientData> clientData ) {
		this.clientData = clientData;
	}

}