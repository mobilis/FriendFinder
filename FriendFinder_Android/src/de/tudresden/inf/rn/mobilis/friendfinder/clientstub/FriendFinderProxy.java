package de.tudresden.inf.rn.mobilis.friendfinder.clientstub;

import java.util.List;import java.util.ArrayList;
import de.tudresden.inf.rn.mobilis.xmpp.beans.XMPPBean;
public class FriendFinderProxy {

	private IFriendFinderOutgoing _bindingStub;


	public FriendFinderProxy( IFriendFinderOutgoing bindingStub) {
		_bindingStub = bindingStub;
	}


	public IFriendFinderOutgoing getBindingStub(){
		return _bindingStub;
	}


	public XMPPBean PositionUpdate( String toJid, ClientData clientData, IXMPPCallback< PositionUpdateResponse > callback ) {
		if ( null == _bindingStub || null == callback )
			return null;

		PositionUpdateRequest out = new PositionUpdateRequest( clientData );
		out.setTo( toJid );

		_bindingStub.sendXMPPBean( out, callback );

		return out;
	}

	public XMPPBean JoinService( String toJid, String clientJID, IXMPPCallback< JoinServiceResponse > callback ) {
		if ( null == _bindingStub || null == callback )
			return null;

		JoinServiceRequest out = new JoinServiceRequest( clientJID );
		out.setTo( toJid );

		_bindingStub.sendXMPPBean( out, callback );

		return out;
	}

	public XMPPBean LeaveService( String toJid, IXMPPCallback< LeaveServiceResponse > callback ) {
		if ( null == _bindingStub || null == callback )
			return null;

		LeaveServiceRequest out = new LeaveServiceRequest(  );
		out.setTo( toJid );

		_bindingStub.sendXMPPBean( out, callback );

		return out;
	}

}