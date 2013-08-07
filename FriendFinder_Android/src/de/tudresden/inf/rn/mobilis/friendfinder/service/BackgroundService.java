package de.tudresden.inf.rn.mobilis.friendfinder.service;

import java.io.StringReader;
import java.util.HashMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import de.tudresden.inf.rn.mobilis.eet.EET;
import de.tudresden.inf.rn.mobilis.eet.IEETProxy;
import de.tudresden.inf.rn.mobilis.eet.ILocationProxy;
import de.tudresden.inf.rn.mobilis.friendfinder.clientstub.ClientData;
import de.tudresden.inf.rn.mobilis.friendfinder.clientstub.ClientLocation;
import de.tudresden.inf.rn.mobilis.friendfinder.clientstub.MUCMessage;
import de.tudresden.inf.rn.mobilis.friendfinder.proxy.IQProxy;
import de.tudresden.inf.rn.mobilis.friendfinder.proxy.MXAProxy;
import de.tudresden.inf.rn.mobilis.mxa.parcelable.XMPPMessage;

/**
 * The BackgroundService holds the classes for communication and tracking
 * there are also some methods for using the xmpp-muc and its data-messages
 * was bound over the ServiceConnector
 */
public class BackgroundService extends Service implements LocationListener {

	private static final String TAG = "BackgroundService";

	private MXAProxy mMXAProxy;
	private IQProxy mIQProxy;
	/**
	 * registered class, which is called, if there is a xmpp-response and no registered callbackclass in the IQProxy
	 */
	private ICallback callbackClass;
	/**
	 * the JID of the MUC
	 */
	private String mucJID;
	/**
	 * password of the MUC
	 */
	private String mucPwd;
	/**
	 * JID of the joined service-instance
	 */
	private String serviceJID;
	/**
	 * tracking-class
	 */
	private ILocationProxy locationProxy;
	/**
	 * own data, like name, color, position, ...
	 */
	private ClientData clientData;
	/**
	 * gathered informations about the other MUC-members
	 */
	private HashMap<String, ClientData> clientDataList;
	/**
	 * namespace of the service
	 */
	private final String ServiceNamespace = "http://mobilis.inf.tu-dresden.de#services/FriendFinder";

	/*************** Handler *****************/
	
	private LocalBinder mBinder = new LocalBinder();
	class LocalBinder extends Binder {
		BackgroundService getService() {
			return BackgroundService.this;
		}
	}
	/**
	 * called, if the location has changed
	 */
	private Handler onLocationChangedHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			if(msg.what == 1){
				Location lastLoc = (Location) msg.obj;
				
				ClientLocation location = clientData.getClientLocation();
				location.setAccuracy(lastLoc.getAccuracy());
				location.setLat(lastLoc.getLatitude());
				location.setLng(lastLoc.getLongitude());
				
				MUCMessage mucMsg = new MUCMessage();
				mucMsg.setClientData(clientData);
				mucMsg.setText("");
				
				sendMUCMessage(mucMsg);
			}
		}
	};

	/******************* service methods ******************/
	
	@Override
	public IBinder onBind(Intent arg0) {
		Log.v(TAG, "onBind()");

		return mBinder;
	}

	@Override
	public void onCreate() {
		Log.v(TAG, "onCreate()");

		mMXAProxy = new MXAProxy(this);
		mIQProxy = new IQProxy(mMXAProxy, this);

		setMucJID("");
		setMucPwd("");

		clientData = new ClientData();
		clientDataList = new HashMap<String, ClientData>();

		locationProxy = new EET(this, this.serviceJID, (IEETProxy) mIQProxy.getProxy());
		locationProxy.registerLocationListener(this);
	}

	@Override
	public void onDestroy() {
		Log.v(TAG, "onDestroy()");

		mMXAProxy.disconnect();
		locationProxy.stop();
	}

	/***************** muc-facade ********************/
	/**
	 * connect to the muc with the saved jid and password
	 */
	public void connectToMUC() {
		if (getMXAProxy().isConnected()) {
			try {
				getMXAProxy().connectToMUC(mucJID, mucPwd);
			} catch (RemoteException e) {
				Log.e(TAG, "connectToMUC", e);
			}
		}
	}
	/**
	 * 
	 * @return true, if muc is successful connected
	 */
	public Boolean isMUCConnected() {
		return getMXAProxy().isConnected() && getMXAProxy().isMUCConnected();
	}
	/**
	 * unconnect muc
	 */
	public void unconnectMUC() {
		try {
			if(getMXAProxy().isConnected() && getMXAProxy().isMUCConnected()) getMXAProxy().getMultiUserChatService().leaveRoom(mucJID);
		} catch (RemoteException e) {
			Log.e(TAG, "unconnectMUC()", e);
		}
	}
	/**
	 * send a data-message to the muc
	 * @param mucMsg
	 */
	public void sendMUCMessage(MUCMessage mucMsg) {
		if (getMXAProxy().isConnected()) {
			//mucMsg.getClientData().setTime(System.currentTimeMillis());
			XMPPMessage xMsg = new XMPPMessage();
			xMsg.type = XMPPMessage.TYPE_GROUPCHAT;
			xMsg.body = mucMsg.toXML();

			try {
				getMXAProxy().getMultiUserChatService().sendGroupMessage(
						mucJID, xMsg);
			} catch (RemoteException e) {
				Log.e(TAG, "sendMUCMessage()", e);
			}
		}
	}
	/**
	 * register a listener, which is called, if there is a new message received from the muc
	 * @param activity current activity
	 * @param handler
	 */
	public void registerMUCListener(Activity activity, Handler handler) {
		if (getMXAProxy().isConnected()) {
			getMXAProxy().registerIncomingMessageObserver(activity, handler, mucJID);
		}
				
	}

	/***************** class-specific functions *****************/
	/**
	 * update the clientDataList with the given instance of ClientData
	 * @param data
	 */
	public void updateClientDataList(ClientData data) {
		// if (clientDataList.get(data.getJid()) == null
		//		|| clientDataList.get(data.getJid()).getTime() < data.getTime())
			clientDataList.put(data.getJid(), data);
	}

	/**
	 * parse XML MUCMessage in ClientData and text
	 * save the received ClientData in the clientDataList
	 * 
	 * @param body
	 *            xml message
	 * @return the text-message
	 */
	public String parseXMLMUCMessage(String body, Boolean updateClientDataList) {
		String text = "";
		if (body.length() > 0 && body.subSequence(0, 1).equals("<")) {

			XmlPullParser xpp;
			try {
				xpp = XmlPullParserFactory.newInstance().newPullParser();
				xpp.setInput(new StringReader(body));

				MUCMessage msg = new MUCMessage();
				msg.fromXML(xpp);

				if(updateClientDataList) updateClientDataList(msg.getClientData());

				text = msg.getText();
			} catch (Exception e) {
				Log.e(TAG, "MUCListAdapter.bindView()", e);
				text = "Error while parsing messge";
			}
		} else {
			text = body;
		}
		return text;
	}

	/***************** Getter/Setter ***********************/

	public MXAProxy getMXAProxy() {
		return mMXAProxy;
	}

	public IQProxy getIQProxy() {
		return mIQProxy;
	}

	public void setCallbackClass(ICallback c) {
		this.callbackClass = c;
	}

	public ICallback getCallbackClass() {
		return this.callbackClass;
	}

	public String getServiceNamespace() {
		return ServiceNamespace;
	}

	public String getMucJID() {
		return mucJID;
	}

	public void setMucJID(String mucJID) {
		this.mucJID = mucJID;
	}

	public String getMucPwd() {
		return mucPwd;
	}

	public void setMucPwd(String mucPwd) {
		this.mucPwd = mucPwd;
	}

	public ClientData getClientData() {
		return clientData;
	}

	public HashMap<String, ClientData> getClientDataList() {
		return clientDataList;
	}

	public ILocationProxy getLocationProxy() {
		return locationProxy;
	}

	public Boolean isSharePosition() {
		return locationProxy.isTracking();
	}
	
	public String getServiceJID() {
		return serviceJID;
	}

	public void setServiceJID(String serviceJID) {
		this.serviceJID = serviceJID;
		this.locationProxy.setServiceJID(serviceJID);
	}

	/****************** LocationListener *****************/

	@Override
	public void onLocationChanged(Location arg0) {
		Message msg = new Message();
		msg.what = 1;
		msg.obj = arg0;
		onLocationChangedHandler.sendMessage(msg);

	}

	@Override
	public void onProviderDisabled(String arg0) { }

	@Override
	public void onProviderEnabled(String arg0) { }

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) { }
}
