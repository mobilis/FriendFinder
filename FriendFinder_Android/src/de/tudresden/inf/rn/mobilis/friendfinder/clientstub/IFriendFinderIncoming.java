package de.tudresden.inf.rn.mobilis.friendfinder.clientstub;

public interface IFriendFinderIncoming {

	void onPositionUpdate( PositionUpdateResponse in );

	void onPositionUpdateError( PositionUpdateRequest in);

	void onJoinService( JoinServiceResponse in );

	void onJoinServiceError( JoinServiceRequest in);

	void onLeaveService( LeaveServiceResponse in );

	void onLeaveServiceError( LeaveServiceRequest in);

}