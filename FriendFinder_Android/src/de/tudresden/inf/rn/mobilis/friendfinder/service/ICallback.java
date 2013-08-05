package de.tudresden.inf.rn.mobilis.friendfinder.service;

import de.tudresden.inf.rn.mobilis.xmpp.beans.XMPPBean;

public interface ICallback {
	public void processPacket(XMPPBean inBean);
}
