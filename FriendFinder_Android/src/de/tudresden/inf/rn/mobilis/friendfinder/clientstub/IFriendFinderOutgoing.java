package de.tudresden.inf.rn.mobilis.friendfinder.clientstub;

import de.tudresden.inf.rn.mobilis.xmpp.beans.XMPPBean;

public interface IFriendFinderOutgoing {

	void sendXMPPBean( XMPPBean out, IXMPPCallback< ? extends XMPPBean > callback );

	void sendXMPPBean( XMPPBean out );

}