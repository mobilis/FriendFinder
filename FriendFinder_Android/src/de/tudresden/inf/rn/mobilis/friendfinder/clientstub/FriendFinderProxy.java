package de.tudresden.inf.rn.mobilis.friendfinder.clientstub;

import de.tudresden.inf.rn.mobilis.eet.IEETProxy;
import de.tudresden.inf.rn.mobilis.xmpp.beans.XMPPInfo;import de.tudresden.inf.rn.mobilis.xmpp.beans.XMPPBean;import java.util.List;import java.util.ArrayList;public class FriendFinderProxy implements IEETProxy{

	private IFriendFinderOutgoing _bindingStub;


	public FriendFinderProxy( IFriendFinderOutgoing bindingStub) {
		_bindingStub = bindingStub;
	}


	public IFriendFinderOutgoing getBindingStub(){
		return _bindingStub;
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

	public XMPPBean IsTrackAvailable( String toJid, ClientLocation position, int trackId, IXMPPCallback< IsTrackAvailableResponse > callback ) {
		if ( null == _bindingStub || null == callback )
			return null;

		IsTrackAvailableRequest out = new IsTrackAvailableRequest( position, trackId );
		out.setTo( toJid );

		_bindingStub.sendXMPPBean( out, callback );

		return out;
	}

}