package de.tudresden.inf.rn.mobilis.friendfinder.proxy;

import de.tudresden.inf.rn.mobilis.xmpp.beans.XMPPBean;public interface IFriendFinderIncoming {

	XMPPBean onJoinService( JoinServiceRequest in );

	XMPPBean onLeaveService( LeaveServiceRequest in );

	XMPPBean onIsTrackAvailable( IsTrackAvailableRequest in );

}