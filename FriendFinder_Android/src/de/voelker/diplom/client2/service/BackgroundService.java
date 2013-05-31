package de.voelker.diplom.client2.service;

import java.io.StringReader;
import java.util.HashMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import de.tudresden.inf.rn.mobilis.friendfinder.clientstub.ClientData;
import de.tudresden.inf.rn.mobilis.friendfinder.clientstub.ClientLocation;
import de.tudresden.inf.rn.mobilis.friendfinder.clientstub.MUCMessage;
import de.tudresden.inf.rn.mobilis.friendfinder.proxy.IQProxy;
import de.tudresden.inf.rn.mobilis.friendfinder.proxy.MXAProxy;
import de.tudresden.inf.rn.mobilis.mxa.parcelable.XMPPMessage;
import de.voelker.diplom.EET.EET;
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
import android.widget.Toast;

public class BackgroundService extends Service implements LocationListener {

	private static final String TAG = "BackgroundService";

	private MXAProxy mMXAProxy;
	private IQProxy mIQProxy;
	private ICallback callbackClass;

	private String mucJID;
	private String mucPwd;
	private String serviceJID;

	private EET locationProxy;

	private ClientData clientData;
	private HashMap<String, ClientData> clientDataList;

	private final String ServiceNamespace = "http://joyo.diskstation.org#services/FriendFinder";

	/*************** Handler *****************/
	
	private LocalBinder mBinder = new LocalBinder();
	class LocalBinder extends Binder {
		BackgroundService getService() {
			return BackgroundService.this;
		}
	}
	
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

		locationProxy = new EET(this);
		locationProxy.start();
		locationProxy.registerLocationListener(this);
	}

	@Override
	public void onDestroy() {
		Log.v(TAG, "onDestroy()");

		mMXAProxy.disconnect();
	}

	/***************** muc-facade ********************/

	public void connectToMUC() {
		if (getMXAProxy().isConnected()) {
		try {
			getMXAProxy().connectToMUC(mucJID, mucPwd);
		} catch (RemoteException e) {
			Log.e(TAG, "connectToMUC", e);
		}
		}
	}

	public Boolean isMUCConnected() {
		return getMXAProxy().isConnected() && getMXAProxy().isMUCConnected();
	}

	public void unconnectMUC() {
		try {
			getMXAProxy().getMultiUserChatService().leaveRoom(mucJID);
		} catch (RemoteException e) {
			Log.e(TAG, "unconnectMUC()", e);
		}
	}

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

	public void registerMUCListener(Activity activity, Handler handler) {
		if (getMXAProxy().isConnected()) {
			getMXAProxy().registerIncomingMessageObserver(activity, handler, mucJID);
		}
				
	}

	/***************** class-specific functions *****************/

	public void updateClientDataList(ClientData data) {
		// if (clientDataList.get(data.getJid()) == null
		//		|| clientDataList.get(data.getJid()).getTime() < data.getTime())
			clientDataList.put(data.getJid(), data);
	}

	/**
	 * parse XML MUCMessage in ClientData and text
	 * 
	 * @param body
	 *            xml message
	 * @return the text-field
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

	public EET getLocationProxy() {
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
	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub

	}



}
