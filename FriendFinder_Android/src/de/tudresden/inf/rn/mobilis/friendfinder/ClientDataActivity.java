package de.tudresden.inf.rn.mobilis.friendfinder;

import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import de.tudresden.inf.rn.mobilis.friendfinder.clientstub.ClientData;
import de.tudresden.inf.rn.mobilis.friendfinder.service.ServiceConnector;

public class ClientDataActivity extends Activity {

	public static final String TAG = "ClientDataActivity";

	private ListView list;
	private ClientDataListAdapter adapter;

	private ServiceConnector mServiceConnector;

	/********************* Handler **********************/

	private Handler onServiceBoundHandler = new Handler() {
		public void handleMessage(Message msg) {

			initList();
			mServiceConnector.getService().registerMUCListener(
					ClientDataActivity.this, onNewMUCMessageHandler);
		}
	};

	private Handler onNewMUCMessageHandler = new Handler() {
		public void handleMessage(Message msg) {
			// Display a toast on top of the screen
			String text = mServiceConnector.getService().parseXMLMUCMessage(
					msg.obj.toString(), true);
			if (text.length() > 0) {
				Toast toast = Toast.makeText(ClientDataActivity.this, text,
						Toast.LENGTH_LONG);
				toast.setGravity(Gravity.TOP, 0, 50);
				toast.show();
			}
			
			adapter.updateData(mServiceConnector.getService().getClientDataList());
		}
	};

	/********************* Activity Methods *********************/

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_cd);

		mServiceConnector = new ServiceConnector(this);
		mServiceConnector.bindService(onServiceBoundHandler);

		list = (ListView) findViewById(R.id.cd_listView);
	}

	@Override
	public void finish() {
		mServiceConnector.unbindService();

		super.finish();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	/*********************** class-spezific functions **********************/

	public void initList() {
		adapter = new ClientDataListAdapter(mServiceConnector
				.getService().getClientDataList(), this);
		list.setAdapter(adapter);
	}

	public class ClientDataListAdapter extends BaseAdapter {

		private HashMap<String, ClientData> mData = new HashMap<String, ClientData>();
		private String[] mKeys;
		private LayoutInflater inflater;

		public ClientDataListAdapter(HashMap<String, ClientData> data, Context ctx) {
			super();
			mData = data;
			mKeys = mData.keySet().toArray(new String[data.size()]);
			inflater = LayoutInflater.from(ctx);
		}
		
		public void updateData(HashMap<String, ClientData> map){
			this.mData = map;
			this.notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return mData.size();
		}

		@Override
		public ClientData getItem(int position) {
			if(position > mKeys.length-1) return null;
			else return mData.get(mKeys[position]);
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		@Override
		public View getView(int pos, View view, ViewGroup parent) {
			ClientData cd = getItem(pos);
			if(cd == null) return view;
			
			if(view == null){
				view = inflater.inflate(R.layout.listitem_muc, null);
			}
			
			View color = view.findViewById(R.id.muc_listitem_color);
			TextView name = (TextView)view.findViewById(R.id.muc_listitem_name);
			TextView jid = (TextView)view.findViewById(R.id.muc_listitem_jid);
			TextView position = (TextView)view.findViewById(R.id.muc_listitem_position);
			
			color.setBackgroundColor(Color.parseColor(cd.getColor()));
			name.setText(cd.getName());
			jid.setText(cd.getJid());
			if(cd.getPositionUpdateEnabled()) position.setText("N" + cd.getClientLocation().getLat()
					+ " E" + cd.getClientLocation().getLng() + " ("
					+ cd.getClientLocation().getAccuracy() + "m)");
			else position.setText("tracking disabled");
			
			return view;
		}
	}
}
