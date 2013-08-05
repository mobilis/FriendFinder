package de.tudresden.inf.rn.mobilis.friendfinder.proxy;

import de.tudresden.inf.rn.mobilis.xmpp.beans.XMPPBean;import org.xmlpull.v1.XmlPullParser;import java.util.List;import java.util.ArrayList;

public class JoinServiceResponse extends XMPPBean {

	private String mucJID = null;
	private String mucPwd = null;
	private String color = null;


	public JoinServiceResponse( String mucJID, String mucPwd, String color ) {
		super();
		this.mucJID = mucJID;
		this.mucPwd = mucPwd;
		this.color = color;

		this.setType( XMPPBean.TYPE_RESULT );
	}

	public JoinServiceResponse(){
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
				else if (tagName.equals( "mucJID" ) ) {
					this.mucJID = parser.nextText();
				}
				else if (tagName.equals( "mucPwd" ) ) {
					this.mucPwd = parser.nextText();
				}
				else if (tagName.equals( "color" ) ) {
					this.color = parser.nextText();
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

	public static final String CHILD_ELEMENT = "JoinServiceResponse";

	@Override
	public String getChildElement() {
		return CHILD_ELEMENT;
	}

	public static final String NAMESPACE = "eet:iq:join_service";

	@Override
	public String getNamespace() {
		return NAMESPACE;
	}

	@Override
	public XMPPBean clone() {
		JoinServiceResponse clone = new JoinServiceResponse( mucJID, mucPwd, color );
		clone.cloneBasicAttributes( clone );

		return clone;
	}

	@Override
	public String payloadToXML() {
		StringBuilder sb = new StringBuilder();

		sb.append( "<mucJID>" )
			.append( this.mucJID )
			.append( "</mucJID>" );

		sb.append( "<mucPwd>" )
			.append( this.mucPwd )
			.append( "</mucPwd>" );

		sb.append( "<color>" )
			.append( this.color )
			.append( "</color>" );

		sb = appendErrorPayload(sb);

		return sb.toString();
	}


	public String getMucJID() {
		return this.mucJID;
	}

	public void setMucJID( String mucJID ) {
		this.mucJID = mucJID;
	}

	public String getMucPwd() {
		return this.mucPwd;
	}

	public void setMucPwd( String mucPwd ) {
		this.mucPwd = mucPwd;
	}

	public String getColor() {
		return this.color;
	}

	public void setColor( String color ) {
		this.color = color;
	}

}