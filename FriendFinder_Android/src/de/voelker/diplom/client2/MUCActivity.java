package de.voelker.diplom.client2;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Locale;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import de.tudresden.inf.rn.mobilis.friendfinder.clientstub.ClientData;
import de.tudresden.inf.rn.mobilis.friendfinder.clientstub.MUCMessage;
import de.tudresden.inf.rn.mobilis.friendfinder.proxy.MXAProxy;
import de.tudresden.inf.rn.mobilis.mxa.ConstMXA;
import de.tudresden.inf.rn.mobilis.mxa.ConstMXA.MessageItems;
import de.tudresden.inf.rn.mobilis.mxa.parcelable.XMPPMessage;
import de.tudresden.inf.rn.mobilis.xmpp.beans.XMPPInfo;
import de.voelker.diplom.client2.service.BackgroundService;
import de.voelker.diplom.client2.service.ServiceConnector;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class MUCActivity extends Activity implements OnClickListener {

	public static final String TAG = "MUCActivity";

	private EditText editText;
	private Button btn_send;
	private Button btn_pos;
	private Button btn_cd;
	private ListView list;

	private Cursor msgCursor;

	private ServiceConnector mServiceConnector;

	/********************* Handler **********************/

	private Handler onServiceBoundHandler = new Handler() {
		public void handleMessage(Message msg) {
			mServiceConnector.getService().connectToMUC();
			initMessageHistory();
			btn_send.setEnabled(true);
			mServiceConnector.getService().registerMUCListener(MUCActivity.this, onNewMUCMessageHandler);
		}
	};
	
	private Handler onNewMUCMessageHandler = new Handler() {
		public void handleMessage(Message msg) {
			// Display a toast on top of the screen
			String text = mServiceConnector.getService().parseXMLMUCMessage(
					msg.obj.toString(), true);
			if (text.length() > 0) {
				/*Toast toast = Toast.makeText(MUCActivity.this, text,
						Toast.LENGTH_LONG);
				toast.setGravity(Gravity.TOP, 0, 50);
				toast.show();*/
			}
		}
	};

	/********************* Activity Methods *********************/

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_muc);

		mServiceConnector = new ServiceConnector(this);
		mServiceConnector.bindService(onServiceBoundHandler);

		btn_send = (Button) findViewById(R.id.muc_btn_send);
		editText = (EditText) findViewById(R.id.muc_editText);
		btn_send.setOnClickListener(this);
		btn_send.setEnabled(false);

		btn_pos = (Button) findViewById(R.id.muc_btn_pos);
		btn_pos.setOnClickListener(this);

		btn_cd = (Button) findViewById(R.id.muc_btn_cd);
		btn_cd.setOnClickListener(this);

		list = (ListView) findViewById(R.id.muc_listView);
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
		mServiceConnector.getService().unconnectMUC();

		super.onDestroy();
	}

	@Override
	public void onClick(View view) {
		if (view != null)
			switch (view.getId()) {
			case R.id.muc_btn_send:
				sendMessage();
				break;
			case R.id.muc_btn_pos:
				Intent intent = new Intent(this, MapActivity.class);
				startActivity(intent);
				break;
			case R.id.muc_btn_cd:
				Intent intent2 = new Intent(this, ClientDataActivity.class);
				startActivity(intent2);

				// Log.v(TAG,
				// mServiceConnector.getService().getClientDataList().toString());
				break;
			}

	}

	/*********************** class-spezific functions **********************/

	/**
	 * Send the Message to the XMPP-Service
	 */

	private void sendMessage() {
		// ignore, if text ist empty
		if (editText.getText().toString().equals(""))
			return;

		BackgroundService bgs = mServiceConnector.getService();
		MUCMessage mucMsg = new MUCMessage();
		ClientData cd = bgs.getClientData();

		if (bgs.isSharePosition()) {
			Location l = bgs.getLocationProxy().getLastLocation();
			if (l != null) {
				cd.getClientLocation().setLat(l.getLatitude());
				cd.getClientLocation().setLng(l.getLongitude());
				cd.getClientLocation().setAccuracy(l.getAccuracy());
			}
		}

		mucMsg.setClientData(cd);
		mucMsg.setText(editText.getText().toString());

		if (bgs.getMXAProxy().isConnected()) {
			mServiceConnector.getService().sendMUCMessage(mucMsg);

			// Clear the EditText
			editText.setText("");
		} else {
			// If XMPP isn't connected notify the user
			Toast toast = Toast
					.makeText(
							MUCActivity.this,
							"Sorry, connection to server lost. Please try again in a moment",
							Toast.LENGTH_LONG);
			toast.setGravity(Gravity.TOP, 0, 0);
			toast.show();
		}
	}

	/**
	 * Inits the message history to display old messages.
	 */
	private void initMessageHistory() {
		list.setAdapter(null);

		// Get a cursor to all messages to the user
		String selectClause = MessageItems.SENDER + " LIKE '"
				+ mServiceConnector.getService().getMucJID() + "%' AND " + MessageItems.BODY + " NOT LIKE '%<text></text>%'";

		msgCursor = getContentResolver().query(MessageItems.CONTENT_URI, null,
				selectClause, null, MessageItems.DEFAULT_SORT_ORDER);
		// Start to manage the cursor
		startManagingCursor(msgCursor);

		// Set up a Listadapter to handle the history messages
		MUCListAdapter adapter = new MUCListAdapter(this,
				android.R.layout.simple_list_item_2, msgCursor, new String[] {
						MessageItems.BODY, MessageItems.SENDER }, new int[] {
						android.R.id.text1, android.R.id.text2 });

		list.setAdapter(adapter);
	}

	private class MUCListAdapter extends SimpleCursorAdapter {

		private String mucJID;
		private int mLayout;

		/**
		 * Constructor for the ListAdapter.
		 * 
		 * @param context
		 *            Context to bind
		 * @param layout
		 *            List style
		 * @param c
		 *            Cursor which points on the data
		 * @param from
		 *            Points on the columns in the database
		 * @param to
		 *            Points on the views in the list
		 */
		public MUCListAdapter(Context context, int layout, Cursor c,
				String[] from, int[] to) {
			super(context, layout, c, from, to);
			mLayout = layout;
			mucJID = mServiceConnector.getService().getMucJID();
		}

		/*
		 * @Override public View newView(Context context, Cursor cursor,
		 * ViewGroup parent) { View view =
		 * LayoutInflater.from(context).inflate(mLayout, parent, false);
		 * 
		 * // Get the current body-entry String body = cursor.getString(cursor
		 * .getColumnIndex(MessageItems.BODY)); // Get the current sender-entry
		 * and cut out the chatID String sender = cursor.getString(cursor
		 * .getColumnIndex(MessageItems.SENDER));
		 * 
		 * // cut the chatroom-jid if (sender != null) { sender =
		 * sender.replace( mucJID.toLowerCase(Locale.getDefault()), ""); if
		 * (sender.length() > 0) sender = sender.substring(1); } else sender =
		 * "null";
		 * 
		 * // get text from xml String text =
		 * mServiceConnector.getService().parseXMLMUCMessage( body);
		 * 
		 * // Catch the TextView's which represents the sender and the body
		 * TextView tv_body = (TextView) view.findViewById(android.R.id.text1);
		 * TextView tv_sender = (TextView) view
		 * .findViewById(android.R.id.text2);
		 * 
		 * if (sender.equals("")) tv_body.setTextColor(Color.GRAY); else
		 * tv_body.setTextColor(Color.BLACK);
		 * 
		 * tv_sender.setTextColor(Color.GRAY);
		 * 
		 * // Bind the text to the corresponding cursor-entry
		 * tv_body.setText(text); tv_sender.setText(sender);
		 * 
		 * if (text == "") return null;
		 * 
		 * return view; }
		 */

		/**
		 * Binds the view and the cursors data and represent it to the context
		 * 
		 * We override the standard SimpleCursorAdapter so that we can modify
		 * the sender to the nickname of the player.
		 * 
		 * @param view
		 *            View to bind on
		 * @param context
		 *            Context for representation
		 * @param cursor
		 *            the cursor
		 */

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			// Get the current body-entry
			String body = cursor.getString(cursor
					.getColumnIndex(MessageItems.BODY));
			// Get the current sender-entry and cut out the chatID
			String sender = cursor.getString(cursor
					.getColumnIndex(MessageItems.SENDER));

			// cut the chatroom-jid
			if (sender != null) {
				sender = sender.replace(
						mucJID.toLowerCase(Locale.getDefault()), "");
				if (sender.length() > 0)
					sender = sender.substring(1);
			} else
				sender = "null";

			// get text from xml
			String text = mServiceConnector.getService().parseXMLMUCMessage(
					body, false);

			// Catch the TextView's which represents the sender and the body
			TextView tv_body = (TextView) view.findViewById(android.R.id.text1);
			TextView tv_sender = (TextView) view
					.findViewById(android.R.id.text2);

			if (sender.equals(""))
				tv_body.setTextColor(Color.GRAY);
			else
				tv_body.setTextColor(Color.BLACK);

			tv_sender.setTextColor(Color.GRAY);

			// Bind the text to the corresponding cursor-entry
			tv_body.setText(text);
			tv_sender.setText(sender);

		}

	}
}
