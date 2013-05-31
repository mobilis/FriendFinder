package de.voelker.diplom.client2;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import de.tudresden.inf.rn.mobilis.friendfinder.clientstub.ClientData;
import de.tudresden.inf.rn.mobilis.mxa.ConstMXA;
import de.voelker.diplom.client2.service.ServiceConnector;

public class ClientDataActivity extends Activity {

	public static final String TAG = "ClientDataActivity";

	private ListView list;

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
			Intent intent2 = new Intent(this, ClientDataActivity.class);
			startActivity(intent2);
			return true;
		}
		return super.onOptionsItemSelected(item);
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
		list.setAdapter(new ClientDataListAdapter(mServiceConnector
				.getService().getClientDataList()));
	}

	public class ClientDataListAdapter extends BaseAdapter {

		private HashMap<String, ClientData> mData = new HashMap<String, ClientData>();
		private String[] mKeys;

		public ClientDataListAdapter(HashMap<String, ClientData> data) {
			super();
			mData = data;
			mKeys = mData.keySet().toArray(new String[data.size()]);
		}

		@Override
		public int getCount() {
			return mData.size();
		}

		@Override
		public ClientData getItem(int position) {
			return mData.get(mKeys[position]);
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		@Override
		public View getView(int pos, View view, ViewGroup parent) {
			String key = mKeys[pos];
			// String value = getItem(pos).toXML();
			ClientData cd = getItem(pos);

			// SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm:ss");
			// Date date = new Date(cd.getTime());

			String value = cd.getName() + "\n" + cd.getColor() + "\n"
					/* + sdf.format(date) + "\n" */ + "N" + cd.getClientLocation().getLat()
					+ " E" + cd.getClientLocation().getLng() + "\n(Accuracy: "
					+ cd.getClientLocation().getAccuracy() + ")";

			// Catch the TextView's which represents the sender and the body

			LinearLayout ll = new LinearLayout(ClientDataActivity.this);
			ll.setOrientation(LinearLayout.VERTICAL);
			ll.setPadding(5, 1, 1, 1);

			TextView tv_body = new TextView(ClientDataActivity.this);
			TextView tv_sender = new TextView(ClientDataActivity.this);
			tv_sender.setTextColor(Color.GRAY);
			tv_sender.setTextSize(10);
			ll.addView(tv_body, new LinearLayout.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			ll.addView(tv_sender, new LinearLayout.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

			// Bind the text to the corresponding cursor-entry
			tv_body.setText(value);
			tv_sender.setText(key);

			return ll;
		}
	}
}
