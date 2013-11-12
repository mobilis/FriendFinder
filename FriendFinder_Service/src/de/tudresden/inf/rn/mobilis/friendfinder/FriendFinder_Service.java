package de.tudresden.inf.rn.mobilis.friendfinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.muc.MultiUserChat;

import de.tudresden.inf.rn.mobilis.friendfinder.proxy.ClientData;
import de.tudresden.inf.rn.mobilis.friendfinder.proxy.ClientDataList;
import de.tudresden.inf.rn.mobilis.friendfinder.proxy.FriendFinderProxy;
import de.tudresden.inf.rn.mobilis.friendfinder.proxy.IFriendFinderIncoming;
import de.tudresden.inf.rn.mobilis.friendfinder.proxy.IFriendFinderOutgoing;
import de.tudresden.inf.rn.mobilis.friendfinder.proxy.IsTrackAvailableRequest;
import de.tudresden.inf.rn.mobilis.friendfinder.proxy.IsTrackAvailableResponse;
import de.tudresden.inf.rn.mobilis.friendfinder.proxy.JoinServiceRequest;
import de.tudresden.inf.rn.mobilis.friendfinder.proxy.JoinServiceResponse;
import de.tudresden.inf.rn.mobilis.friendfinder.proxy.LeaveServiceRequest;
import de.tudresden.inf.rn.mobilis.friendfinder.proxy.LeaveServiceResponse;
import de.tudresden.inf.rn.mobilis.server.services.MobilisService;
import de.tudresden.inf.rn.mobilis.xmpp.beans.IXMPPCallback;
import de.tudresden.inf.rn.mobilis.xmpp.beans.ProxyBean;
import de.tudresden.inf.rn.mobilis.xmpp.beans.XMPPBean;
import de.tudresden.inf.rn.mobilis.xmpp.beans.deployment.PingBean;
import de.tudresden.inf.rn.mobilis.xmpp.server.BeanHelper;
import de.tudresden.inf.rn.mobilis.xmpp.server.BeanIQAdapter;
import de.tudresden.inf.rn.mobilis.xmpp.server.BeanProviderAdapter;

/**
 * the entry-class of this service
 *
 */
public class FriendFinder_Service extends MobilisService {

	private final static Logger LOG = Logger.getLogger(FriendFinder_Service.class
			.getCanonicalName());
	
	/**
	 * proxy-class for sending iqs
	 */
	private FriendFinderProxy _proxy;
	/**
	 * waiting callbacks
	 */
	private Map<String, IXMPPCallback<? extends XMPPBean>> _waitingCallbacks;

	/**
	 * save all joined clients
	 */
	private HashMap<String, ClientData> clientJID;

	private MultiUserChat muc;
	private String mucDomain = "conference.mobilis.inf.tu-dresden.de";
	private String mucPwd = "test";
	private long serviceID = 1;
	private String serviceName = "FriendFinder";
	private String mucJID = "test@" + mucDomain;
	/**
	 * available colors for the map-marker
	 */
	private String[] colors = { "#ff0000", "#00ff00", "#0000ff", "#ffff00",
			"#ff00ff", "#00ffff" };
	/**
	 * last used color
	 */
	private int lastColor = 0;
	
	/**
	 * eet-service with the tracking-algorithm
	 */
	private EET_Service eet;

	public FriendFinder_Service() {
		_proxy = new FriendFinderProxy(_FriendFinderServiceOutgoingStub);
		_waitingCallbacks = new HashMap<String, IXMPPCallback<? extends XMPPBean>>();

		clientJID = new HashMap<String, ClientData>();
		
		eet = new EET_Service();
	}

	@Override
	public void startup() throws Exception {
		super.startup();
		eet.startup();

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
		(new BeanProviderAdapter(new ProxyBean(LeaveServiceRequest.NAMESPACE,
				LeaveServiceRequest.CHILD_ELEMENT))).addToProviderManager();

		eet.registerPacketListener();
		
		IQListener iqListener = new IQListener();
		PacketTypeFilter locFil = new PacketTypeFilter(IQ.class);
		getAgent().getConnection().addPacketListener(iqListener, locFil);

	}

	@Override
	public void shutdown() {
		eet.shutdown();
		
		LOG.info("shutting down");
	}

	/*************** class-specific functions **************************/
	/**
	 * create a new muc
	 * @throws XMPPException
	 */
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

	/**
	 * generate a random password for every muc
	 * @return
	 */
	private String generatePwd() {
		return "test!Pwd" + System.currentTimeMillis();
	}

	/*************** external classes *************************/
	/**
	 * process the outgoing xmpp-beans
	 */
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

	/**
	 * process the incoming xmpp-beans
	 */
	private IFriendFinderIncoming _FriendFinderIncomingStub = new IFriendFinderIncoming() {

		@Override
		public XMPPBean onJoinService(JoinServiceRequest in) {
			LOG.info(in.getFrom() + " has joined service");

			ClientData cd = new ClientData();
			cd.setColor(colors[(++lastColor) % colors.length]);
			clientJID.put(in.getClientJID(), cd);

			JoinServiceResponse out = new JoinServiceResponse();
			out.setId(in.getId());
			out.setMucJID(mucJID);
			out.setMucPwd(mucPwd);
			out.setColor(cd.getColor());
			return out;
		}

		@Override
		public XMPPBean onLeaveService(LeaveServiceRequest in) {
			LOG.info(in.getFrom() + "  leave the service");
			
			if (clientJID.containsKey(in.getFrom()))
				clientJID.remove(in.getFrom());

			LeaveServiceResponse out = new LeaveServiceResponse();
			out.setId(in.getId());
			return out;
		}

		/**
		 * do nothing, this method is implemented in the EET_Service
		 */
		@Override
		public XMPPBean onIsTrackAvailable(IsTrackAvailableRequest in) {
			return null;
		}
	};

	/**
	 * IQListener for processing the incoming XMPPBeans
	 *
	 */
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

					if (eet.processPacket(proxyBean, _proxy)){
						// do nothing
					} else if (proxyBean.isTypeOf(JoinServiceRequest.NAMESPACE,
							JoinServiceRequest.CHILD_ELEMENT)) {

						JoinServiceResponse response = (JoinServiceResponse) _FriendFinderIncomingStub
								.onJoinService((JoinServiceRequest) proxyBean
										.parsePayload(new JoinServiceRequest()));

						_proxy.JoinService(proxyBean.getFrom(),
								response.getId(), response.getMucJID(),
								response.getMucPwd(), response.getColor());

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
	}

	public List<PacketExtension> getNodePacketExtensions() {
		// TODO Auto-generated method stub
		return null;
	};
}
