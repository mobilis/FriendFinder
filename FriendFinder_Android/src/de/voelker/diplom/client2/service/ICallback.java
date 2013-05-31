package de.voelker.diplom.client2.service;

import de.tudresden.inf.rn.mobilis.xmpp.beans.XMPPBean;

public interface ICallback {
	public void processPacket(XMPPBean inBean);
}
