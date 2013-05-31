package de.tudresden.inf.rn.mobilis.friendfinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.muc.MultiUserChat;

import de.tudresden.inf.rn.mobilis.friendfinder.proxy.ClientData;
import de.tudresden.inf.rn.mobilis.friendfinder.proxy.ClientDataList;
import de.tudresden.inf.rn.mobilis.friendfinder.proxy.FriendFinderProxy;
import de.tudresden.inf.rn.mobilis.friendfinder.proxy.IFriendFinderIncoming;
import de.tudresden.inf.rn.mobilis.friendfinder.proxy.IFriendFinderOutgoing;
import de.tudresden.inf.rn.mobilis.friendfinder.proxy.JoinServiceRequest;
import de.tudresden.inf.rn.mobilis.friendfinder.proxy.JoinServiceResponse;
import de.tudresden.inf.rn.mobilis.friendfinder.proxy.LeaveServiceRequest;
import de.tudresden.inf.rn.mobilis.friendfinder.proxy.LeaveServiceResponse;
import de.tudresden.inf.rn.mobilis.friendfinder.proxy.PositionUpdateRequest;
import de.tudresden.inf.rn.mobilis.friendfinder.proxy.PositionUpdateResponse;
import de.tudresden.inf.rn.mobilis.server.services.MobilisService;
import de.tudresden.inf.rn.mobilis.xmpp.beans.IXMPPCallback;
import de.tudresden.inf.rn.mobilis.xmpp.beans.ProxyBean;
import de.tudresden.inf.rn.mobilis.xmpp.beans.XMPPBean;
import de.tudresden.inf.rn.mobilis.xmpp.beans.admin.PingBean;
import de.tudresden.inf.rn.mobilis.xmpp.server.BeanHelper;
import de.tudresden.inf.rn.mobilis.xmpp.server.BeanIQAdapter;
import de.tudresden.inf.rn.mobilis.xmpp.server.BeanProviderAdapter;

public class FriendFinder_Service extends MobilisService {

	private FriendFinderProxy _proxy;
	private Map<String, IXMPPCallback<? extends XMPPBean>> _waitingCallbacks;

	private Map<String, ClientData> clients;
	private ArrayList<String> clientJID;

	private MultiUserChat muc;
	private String mucDomain = "conference.joyo.diskstation.org";
	private String mucPwd = "test";
	private long serviceID = 1;
	private String serviceName = "FriendFinder";
	private String mucJID = "test@" + mucDomain;

	private String[] colors = { "#ff0000", "#00ff00", "#0000ff", "#ffff00",
			"#ff00ff", "#00ffff" };

	private final static Logger LOG = Logger.getLogger(FriendFinder_Service.class
			.getCanonicalName());

	public FriendFinder_Service() {
		_proxy = new FriendFinderProxy(_FriendFinderServiceOutgoingStub);
		_waitingCallbacks = new HashMap<String, IXMPPCallback<? extends XMPPBean>>();

		clients = Collections
				.synchronizedMap(new HashMap<String, ClientData>());
		clientJID = new ArrayList<String>();
	}

	@Override
	public void startup() throws Exception {
		super.startup();

		serviceName = getAgent().getIdent();
		serviceID = System.currentTimeMillis();
		mucJID = serviceName + "_" + serviceID + "@" + mucDomain;
		mucPwd = generatePwd();

		createMUC();

		LOG.info("Echo Service startup");
	}

	@Override
	protected void registerPacketListener() {

		(new BeanProviderAdapter(new ProxyBean(JoinServiceRequest.NAMESPACE,
				JoinServiceRequest.CHILD_ELEMENT))).addToProviderManager();
		(new BeanProviderAdapter(new ProxyBean(PositionUpdateRequest.NAMESPACE,
				PositionUpdateRequest.CHILD_ELEMENT))).addToProviderManager();

		IQListener iqListener = new IQListener();
		PacketTypeFilter locFil = new PacketTypeFilter(IQ.class);
		getAgent().getConnection().addPacketListener(iqListener, locFil);

	}

	@Override
	public void shutdown() {
		LOG.info("shutting down");
	}

	/*************** class-specific functions **************************/

	private void createMUC() throws XMPPException {
		muc = new MultiUserChat(getAgent().getConnection(), mucJID);

		muc.create("Server");

		Form oldForm = muc.getConfigurationForm();
		Form newForm = oldForm.createAnswerForm();

		for (Iterator<FormField> fields = oldForm.getFields(); fields.hasNext();) {
			FormField field = (FormField) fields.next();
			if (!FormField.TYPE_HIDDEN.equals(field.getType())
					&& field.getVariable() != null) {
				newForm.setDefaultAnswer(field.getVariable());
			}
		}

		newForm.setAnswer("muc#roomconfig_passwordprotectedroom", true);
		newForm.setAnswer("muc#roomconfig_roomsecret", mucPwd);

		muc.sendConfigurationForm(newForm);

		LOG.info("Chatroom created (JID: " + mucJID + ", Pw: " + mucPwd + ")");

	}

	private String generatePwd() {
		return "test!Pwd";
	}

	/*************** external classes *************************/

	private IFriendFinderOutgoing _FriendFinderServiceOutgoingStub = new IFriendFinderOutgoing() {

		@Override
		public void sendXMPPBean(XMPPBean out,
				IXMPPCallback<? extends XMPPBean> callback) {
			_waitingCallbacks.put(out.getId(), callback);
			getAgent().getConnection().sendPacket(new BeanIQAdapter(out));
		}

		@Override
		public void sendXMPPBean(XMPPBean out) {
			getAgent().getConnection().sendPacket(new BeanIQAdapter(out));
		}

	};

	private IFriendFinderIncoming _FriendFinderIncomingStub = new IFriendFinderIncoming() {

		@Override
		public XMPPBean onPositionUpdate(PositionUpdateRequest in) {

			// Throw Error, if client not in list?
			clients.put(in.getFrom(), in.getClientData());

			ClientDataList cdl = new ClientDataList(new ArrayList<ClientData>(
					clients.values()));

			PositionUpdateResponse ob = new PositionUpdateResponse();
			ob.setId(in.getId());
			ob.setClientData(cdl);
			return ob;
		}

		@Override
		public XMPPBean onJoinService(JoinServiceRequest in) {

			LOG.info(in.getFrom() + " has joined service");

			if (!clientJID.contains(in.getClientJID()))
				clientJID.add(in.getClientJID());
			int no = clientJID.indexOf(in.getClientJID());

			JoinServiceResponse out = new JoinServiceResponse();
			out.setId(in.getId());
			out.setMucJID(mucJID);
			out.setMucPwd(mucPwd);
			out.setColor(colors[( no % colors.length)]);

			// LOG.info("onJoinService2");

			return out;

		}

		@Override
		public XMPPBean onLeaveService(LeaveServiceRequest in) {
			LOG.info(in.getFrom() + "  leave the service");
			
			if (clientJID.contains(in.getFrom()))
				clientJID.remove(in.getFrom());

			// todo
			LeaveServiceResponse out = new LeaveServiceResponse();
			out.setId(in.getId());

			return out;
		}

	};

	private class IQListener implements PacketListener {

		@Override
		public void processPacket(Packet packet) {
			LOG.info("Incoming packet: " + packet.toXML());

			if (packet instanceof BeanIQAdapter) {
				XMPPBean inBean = ((BeanIQAdapter) packet).getBean();

				// LOG.info("XMPPBean instanceOf: " +
				// inBean.getClass().toString());

				if (inBean instanceof ProxyBean) {
					ProxyBean proxyBean = (ProxyBean) inBean;
					// LOG.info("Namespace: " + proxyBean.getNamespace() +
					// " ChildElement: " + proxyBean.getChildElement());

					if (proxyBean.isTypeOf(PositionUpdateRequest.NAMESPACE,
							PositionUpdateRequest.CHILD_ELEMENT)) {

						PositionUpdateResponse response = (PositionUpdateResponse) _FriendFinderIncomingStub
								.onPositionUpdate((PositionUpdateRequest) proxyBean
										.parsePayload(new PositionUpdateRequest()));

						// Send response
						_proxy.PositionUpdate(proxyBean.getFrom(),
								response.getId(), response.getClientData());

					} else if (proxyBean.isTypeOf(JoinServiceRequest.NAMESPACE,
							JoinServiceRequest.CHILD_ELEMENT)) {

						// LOG.info("JoinServiceResponse1");

						// Forward incoming Bean to ITreasureHuntIncoming and
						// receive response
						JoinServiceResponse response = (JoinServiceResponse) _FriendFinderIncomingStub
								.onJoinService((JoinServiceRequest) proxyBean
										.parsePayload(new JoinServiceRequest()));

						// LOG.info("JoinServiceResponse2");

						// Send response
						_proxy.JoinService(proxyBean.getFrom(),
								response.getId(), response.getMucJID(),
								response.getMucPwd(), response.getColor());

						// LOG.info("JoinServiceResponse3");
					} else if (proxyBean.isTypeOf(LeaveServiceRequest.NAMESPACE,
							LeaveServiceRequest.CHILD_ELEMENT)) {

						LeaveServiceResponse response = (LeaveServiceResponse) _FriendFinderIncomingStub
								.onLeaveService((LeaveServiceRequest) proxyBean
										.parsePayload(new LeaveServiceRequest()));

						_proxy.LeaveService(proxyBean.getFrom(),
								response.getId());

					} else if (proxyBean.isTypeOf(PingBean.NAMESPACE,
							PingBean.CHILD_ELEMENT)) {
						LOG.info("PingBean arrived");

						PingBean pingBean = new PingBean(
								System.currentTimeMillis(),
								"Hello EnEfPosition_Service Version 1");
						pingBean.setTo(inBean.getFrom());
						pingBean.setId(inBean.getId());
						pingBean.setType(XMPPBean.TYPE_RESULT);

						getAgent().getConnection().sendPacket(
								new BeanIQAdapter(pingBean));
					} else {
						getAgent().getConnection().sendPacket(
								new BeanIQAdapter(BeanHelper.CreateErrorBean(
										inBean, "wait", "unexpected-request",
										"This request is not supported")));
					}
				}
			}

		}

	};

}
