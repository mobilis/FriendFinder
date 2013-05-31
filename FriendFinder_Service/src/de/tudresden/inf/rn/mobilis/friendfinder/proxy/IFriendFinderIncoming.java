package de.tudresden.inf.rn.mobilis.friendfinder.proxy;

import de.tudresden.inf.rn.mobilis.xmpp.beans.XMPPBean;

public interface IFriendFinderIncoming {

	XMPPBean onPositionUpdate( PositionUpdateRequest in );

	XMPPBean onJoinService( JoinServiceRequest in );

	XMPPBean onLeaveService( LeaveServiceRequest in );

}