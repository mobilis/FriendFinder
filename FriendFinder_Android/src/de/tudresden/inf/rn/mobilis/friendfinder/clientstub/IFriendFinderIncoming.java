package de.tudresden.inf.rn.mobilis.friendfinder.clientstub;

import de.tudresden.inf.rn.mobilis.xmpp.beans.XMPPInfo;import de.tudresden.inf.rn.mobilis.xmpp.beans.XMPPBean;public interface IFriendFinderIncoming {

	void onJoinService( JoinServiceResponse in );

	void onJoinServiceError( JoinServiceRequest in);

	void onLeaveService( LeaveServiceResponse in );

	void onLeaveServiceError( LeaveServiceRequest in);

	void onIsTrackAvailable( IsTrackAvailableResponse in );

	void onIsTrackAvailableError( IsTrackAvailableRequest in);

}