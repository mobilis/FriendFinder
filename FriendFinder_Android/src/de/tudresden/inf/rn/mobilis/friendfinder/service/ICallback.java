package de.tudresden.inf.rn.mobilis.friendfinder.service;

import de.tudresden.inf.rn.mobilis.xmpp.beans.XMPPBean;

/**
 * Interface to receive and process a xmpp-message
 * register this to the BackgroundService to receive the XMPPBeans
 */
public interface ICallback {
	public void processPacket(XMPPBean inBean);
}
