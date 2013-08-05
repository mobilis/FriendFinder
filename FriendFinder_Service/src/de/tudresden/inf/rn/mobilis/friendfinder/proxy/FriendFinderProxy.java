package de.tudresden.inf.rn.mobilis.friendfinder.proxy;

import java.util.List;import java.util.ArrayList;
import de.tudresden.inf.rn.mobilis.friendfinder.GPXTrack;
import de.tudresden.inf.rn.mobilis.xmpp.beans.XMPPBean;public class FriendFinderProxy {

	private IFriendFinderOutgoing _bindingStub;


	public FriendFinderProxy( IFriendFinderOutgoing bindingStub) {
		_bindingStub = bindingStub;
	}


	public IFriendFinderOutgoing getBindingStub(){
		return _bindingStub;
	}


	public XMPPBean JoinService( String toJid, String packetId, String mucJID, String mucPwd, String color ) {
		if ( null == _bindingStub )
			return null;

		JoinServiceResponse out = new JoinServiceResponse( mucJID, mucPwd, color );
		out.setTo( toJid );
		out.setId( packetId );

		_bindingStub.sendXMPPBean( out );

		return out;
	}

	public XMPPBean LeaveService( String toJid, String packetId ) {
		if ( null == _bindingStub )
			return null;

		LeaveServiceResponse out = new LeaveServiceResponse(  );
		out.setTo( toJid );
		out.setId( packetId );

		_bindingStub.sendXMPPBean( out );

		return out;
	}

	public XMPPBean IsTrackAvailable( String toJid, String packetId, boolean result, int trackId, GPXTrack gpxTrack ) {
		if ( null == _bindingStub )
			return null;

		IsTrackAvailableResponse out = new IsTrackAvailableResponse( result, trackId, gpxTrack );
		out.setTo( toJid );
		out.setId( packetId );

		_bindingStub.sendXMPPBean( out );

		return out;
	}

}