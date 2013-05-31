package de.voelker.diplom.client2;

import java.util.ArrayList;

import com.google.android.gms.common.GooglePlayServicesUtil;

import de.tudresden.inf.rn.mobilis.friendfinder.clientstub.ClientData;
import de.tudresden.inf.rn.mobilis.friendfinder.clientstub.ClientLocation;
import de.tudresden.inf.rn.mobilis.friendfinder.clientstub.IXMPPCallback;
import de.tudresden.inf.rn.mobilis.friendfinder.clientstub.JoinServiceResponse;
import de.tudresden.inf.rn.mobilis.friendfinder.proxy.MXAProxy;
import de.tudresden.inf.rn.mobilis.mxa.ConstMXA;
import de.tudresden.inf.rn.mobilis.mxa.services.multiuserchat.IMultiUserChatService;
import de.tudresden.inf.rn.mobilis.xmpp.beans.XMPPBean;
import de.tudresden.inf.rn.mobilis.xmpp.beans.coordination.MobilisServiceDiscoveryBean;
import de.tudresden.inf.rn.mobilis.xmpp.beans.coordination.MobilisServiceInfo;
import de.voelker.diplom.client2.service.BackgroundService;
import de.voelker.diplom.client2.service.ICallback;
import de.voelker.diplom.client2.service.ServiceConnector;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements ICallback,
		OnClickListener, OnItemClickListener, SettingsDialogFragment.SettingsDialogListener {

	public static final String TAG = "MainActivity";

	protected BackgroundService mService;

	private TextView text;
	private Button btn_create;
	private Button btn_discover;
	private ListView service_list;

	private ArrayList<String> services;
	private ArrayAdapter<String> adapter;

	private ServiceConnector mServiceConnector;
	private ProgressDialog progressDialog;
	
	private SettingsDialogFragment settingsDialog;
	
	public String tempServiceJID;

	/**************** Handler *********************/

	private Handler mServiceDiscoveryHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 1) {
				services.clear();
				services.addAll((ArrayList<String>) msg.obj);
				adapter.notifyDataSetChanged();
			} else
				Toast.makeText(MainActivity.this, "No suitable service found (Code: " + msg.what + ")",
						Toast.LENGTH_LONG).show();
		}
	};

	private Handler mServiceCreateHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case -1:
				Toast.makeText(MainActivity.this,
						"Fehler beim Erstellen einer neuen Service-Instance",
						Toast.LENGTH_LONG).show();
				break;
			case 1:
				Toast.makeText(MainActivity.this,
						"Neue Service-Instance erfolgreich erstellt.",
						Toast.LENGTH_LONG).show();
				break;
			}
			sendDiscoveryIQ();
		}
	};

	private Handler onServiceBoundHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Log.v(TAG, "onServiceBoundHandler");

			mService = mServiceConnector.getService();
			mService.setCallbackClass(MainActivity.this);
			connectToXMPP();
		}
	};

	private Handler onXMPPConnectHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Log.v(TAG, "onXMPPConnectHandler");

			mServiceConnector.getService().getIQProxy().registerCallbacks();
			sendDiscoveryIQ();

			btn_create.setEnabled(true);
			btn_discover.setEnabled(true);
			
			progressDialog.hide();
		}
	};

	IXMPPCallback<JoinServiceResponse> onJoinService = new IXMPPCallback<JoinServiceResponse>() {
		@Override
		public void invoke(JoinServiceResponse xmppBean) {
			
			Log.v(TAG, "onJoinService()");
			
			BackgroundService service = mServiceConnector.getService();
			service.setMucJID(xmppBean.getMucJID());
			service.setMucPwd(xmppBean.getMucPwd());
			service.setServiceJID(xmppBean.getFrom());
			MXAProxy mxaProxy = mServiceConnector.getService().getMXAProxy();
			ClientData cd = service.getClientData();
			
			cd.setJid(mxaProxy.getXmppJid());
			cd.setName(mxaProxy.getNickname());
			cd.setColor(xmppBean.getColor());
			cd.setClientLocation(new ClientLocation());
			
			Intent intent2 = new Intent(MainActivity.this, MUCActivity.class);
			startActivity(intent2);
		}
	};

	/************************ Activity Methods **********************/

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		text = (TextView) findViewById(R.id.main_text);

		btn_create = (Button) findViewById(R.id.main_btn_create);
		btn_create.setEnabled(false);
		btn_create.setOnClickListener(this);

		btn_discover = (Button) findViewById(R.id.main_btn_discover);
		btn_discover.setEnabled(false);
		btn_discover.setOnClickListener(this);

		services = new ArrayList<String>();
		service_list = (ListView) findViewById(R.id.main_listView);
		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, services);
		adapter.setNotifyOnChange(true);
		service_list.setAdapter(adapter);
		service_list.setOnItemClickListener(this);

		mServiceConnector = new ServiceConnector(this);
		progressDialog = ProgressDialog.show(this, "Connecting", "Wait for XMPP-Connection", true);
		progressDialog.setCancelable(true);
		
		settingsDialog = new SettingsDialogFragment();
		settingsDialog.setCancelable(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			Intent i = new Intent(ConstMXA.INTENT_PREFERENCES);
			this.startActivity(Intent.createChooser(i,
					"MXA not found. Please install."));
			return true;
		case R.id.menu_main:
			Intent intent = new Intent(this, MainActivity.class);
			startActivity(intent);
			finish();
			return true;
		case R.id.menu_muc:
			Intent intent2 = new Intent(this, MUCActivity.class);
			startActivity(intent2);
			return true;
		case R.id.menu_map:
			Intent intent3 = new Intent(this, MapActivity.class);
			startActivity(intent3);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onStart() {
		super.onStart();
		if (!mServiceConnector.isServiceBound())
			mServiceConnector.bindService(onServiceBoundHandler);
	}

	@Override
	public void onStop() {
		super.onStop();
		
		if(progressDialog != null) progressDialog.hide();
		if(settingsDialog != null && settingsDialog.isVisible()) settingsDialog.dismiss();
	}
	
	@Override
	public void onDestroy(){
		mServiceConnector.unbindService();
		
		super.onDestroy();
	}
	
	@Override
	public void finish() {
		// If local Service is up, unregister all IQ-Listeners and stop the local Service
		if(mServiceConnector != null
				&& mServiceConnector.getService() != null){
			
			BackgroundService service = mServiceConnector.getService();
			
			//if(!service.getServiceJID().equals("")) service.getIQProxy().getProxy().LeaveService(service.getServiceJID(), null);
			service.getIQProxy().unregisterCallbacks();
			service.stopSelf();
			mServiceConnector.unbindService();
		}
		
		super.finish();
	}

	@Override
	public void onClick(View arg0) {
		if (arg0 != null)
			switch (arg0.getId()) {
			case R.id.main_btn_create:
				String serviceName = "FriendFinder_1";
				mServiceConnector
				.getService()
				.getIQProxy()
				.sendCreateNewServiceInstanceIQ(
					mServiceConnector.getService()
							.getServiceNamespace(), serviceName, "");
				return;
			case R.id.main_btn_discover:
				sendDiscoveryIQ();
				return;
			}

	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		this.tempServiceJID = services.get(arg2);
		
		settingsDialog.serviceInfoString = "JID: " + tempServiceJID;
		settingsDialog.editNameString = mServiceConnector.getService().getMXAProxy().getNickname();
		settingsDialog.show(MainActivity.this.getFragmentManager(), "SettingsDialogFragment");
	}

	/****************** class-specific functions ************************/
	
	protected void connectToXMPP() {
		MXAProxy mxap = mService.getMXAProxy();
		mxap.registerXMPPConnectHandler(onXMPPConnectHandler);
		try {
			mxap.connect();
		} catch (RemoteException e) {
			Log.e(TAG, "connectToXMPP", e);
		}
	}
	
	protected void sendDiscoveryIQ(){
		mServiceConnector
		.getService()
		.getIQProxy()
		.sendServiceDiscoveryIQ(
				mServiceConnector.getService()
						.getServiceNamespace());
	}

	@Override
	public void processPacket(XMPPBean inBean) {
		if (inBean.getType() == XMPPBean.TYPE_ERROR) {
			Log.e(TAG, "IQ Type ERROR: " + inBean.toXML());
		}

		if (inBean instanceof MobilisServiceDiscoveryBean) {
			MobilisServiceDiscoveryBean bean = (MobilisServiceDiscoveryBean) inBean;

			// If responded MobilisServiceDiscoveryBean is not of kind ERROR,
			// check Mobilis-Server response for XHunt support
			if (bean != null && bean.getType() != XMPPBean.TYPE_ERROR) {
				if (bean.getDiscoveredServices() != null) {
					ArrayList<String> services = new ArrayList<String>();

					for (MobilisServiceInfo info : bean.getDiscoveredServices()) {
						/*
						 * if (info.getNamespace().equals(
						 * mServiceConnector.getService()
						 * .getServiceNamespace()))
						 */
						services.add(info.getJid()); // info.getServiceName() + " (" + info.getJid() + ")");
						Log.v(TAG, info.getJid());
					}

					if (services.size() > 0) {
						Message m = new Message();
						m.obj = services;
						m.what = 1;
						mServiceDiscoveryHandler.sendMessage(m);
					} else {
						mServiceDiscoveryHandler.sendEmptyMessage(-1);
					}
				}
			} else if (bean.getType() == XMPPBean.TYPE_ERROR) {
				mServiceDiscoveryHandler.sendEmptyMessage(0);
			}

		} else if (inBean instanceof MobilisServiceDiscoveryBean) {
			if (inBean.getType() == inBean.TYPE_ERROR) {
				mServiceCreateHandler.sendEmptyMessage(-1);
			} else {
				mServiceCreateHandler.sendEmptyMessage(1);
			}
		}
		// Other Beans of type get or set will be responded with an ERROR
		else {
			Log.e(TAG, "Unexpected Bean in MainActivity: " + inBean.toString());
		}

	}

	/***************** SettingsDialog *********************/
	
	@Override
	public void onDialogPositiveClick(SettingsDialogFragment dialog) {
		String name = dialog.editName.getText().toString();
		
		mServiceConnector.getService().getClientData().setName(name);
		
		String clientJID = mServiceConnector.getService().getMXAProxy()
				.getXmppJid();
		mServiceConnector.getService().getIQProxy().getProxy()
			.JoinService(tempServiceJID, clientJID, onJoinService);
		
		progressDialog.setMessage("Wait for Service-Response");
		progressDialog.show();
		
	}

	@Override
	public void onDialogNegativeClick(SettingsDialogFragment dialog) {
		// Toast.makeText(this, "cancel", Toast.LENGTH_LONG).show();
		tempServiceJID = "";
	}

}
