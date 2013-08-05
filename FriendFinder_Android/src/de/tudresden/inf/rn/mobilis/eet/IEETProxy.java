package de.tudresden.inf.rn.mobilis.eet;

import de.tudresden.inf.rn.mobilis.friendfinder.clientstub.ClientLocation;
import de.tudresden.inf.rn.mobilis.friendfinder.clientstub.IXMPPCallback;
import de.tudresden.inf.rn.mobilis.friendfinder.clientstub.IsTrackAvailableResponse;
import de.tudresden.inf.rn.mobilis.xmpp.beans.XMPPBean;

public interface IEETProxy {
	public XMPPBean IsTrackAvailable( String toJid, ClientLocation position, int trackId, IXMPPCallback< IsTrackAvailableResponse > callback );
}
