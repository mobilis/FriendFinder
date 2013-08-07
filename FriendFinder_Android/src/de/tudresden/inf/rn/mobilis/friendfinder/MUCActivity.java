package de.tudresden.inf.rn.mobilis.friendfinder;

import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import de.tudresden.inf.rn.mobilis.friendfinder.clientstub.ClientData;
import de.tudresden.inf.rn.mobilis.friendfinder.clientstub.IXMPPCallback;
import de.tudresden.inf.rn.mobilis.friendfinder.clientstub.LeaveServiceResponse;
import de.tudresden.inf.rn.mobilis.friendfinder.clientstub.MUCMessage;
import de.tudresden.inf.rn.mobilis.friendfinder.service.BackgroundService;
import de.tudresden.inf.rn.mobilis.friendfinder.service.ServiceConnector;
import de.tudresden.inf.rn.mobilis.mxa.ConstMXA.MessageItems;

/**
 * MUC-activity
 *
 */
public class MUCActivity extends Activity implements OnClickListener,
		CompoundButton.OnCheckedChangeListener {

	public static final String TAG = "MUCActivity";

	private EditText editText;
	private Button btn_send;
	private Button btn_pos;
	private Button btn_cd;
	private ListView list;
	private Switch gps_switch;

	private Cursor msgCursor;

	private ServiceConnector mServiceConnector;

	/********************* Handler **********************/

	/**
	 * called, if the BackgroundService is bound successfully
	 */
	private Handler onServiceBoundHandler = new Handler() {
		public void handleMessage(Message msg) {
			mServiceConnector.getService().connectToMUC();
			initMessageHistory();
			btn_send.setEnabled(true);
			mServiceConnector.getService().registerMUCListener(
					MUCActivity.this, onNewMUCMessageHandler);
		}
	};

	/**
	 * called, if a new muc-message was received 
	 */
	private Handler onNewMUCMessageHandler = new Handler() {
		public void handleMessage(Message msg) {
			// Display a toast on top of the screen
			String text = mServiceConnector.getService().parseXMLMUCMessage(
					msg.obj.toString(), true);
			if (text.length() > 0 && text != "null") {
				/*
				 * Toast toast = Toast.makeText(MUCActivity.this, text,
				 * Toast.LENGTH_LONG); toast.setGravity(Gravity.TOP, 0, 50);
				 * toast.show();
				 */
			}
		}
	};

	/**
	 * called, if a response for the LeaveService-Request received
	 */
	IXMPPCallback<LeaveServiceResponse> onLeaveService = new IXMPPCallback<LeaveServiceResponse>() {
		@Override
		public void invoke(LeaveServiceResponse xmppBean) {
			Log.v(TAG, "onLeaveService()");
		}
	};

	/********************* Activity Methods *********************/

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_muc);

		mServiceConnector = new ServiceConnector(this);
		mServiceConnector.bindService(onServiceBoundHandler);

		editText = (EditText) findViewById(R.id.muc_editText);
		editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
		editText.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (((event.getAction() == KeyEvent.ACTION_DOWN)
						&& (keyCode == KeyEvent.KEYCODE_ENTER))
						|| (keyCode == EditorInfo.IME_ACTION_DONE)) {
					sendMessage();
					return true;
				}
				return false;
			}

		});

		btn_send = (Button) findViewById(R.id.muc_btn_send);
		btn_send.setOnClickListener(this);
		btn_send.setEnabled(false);

		btn_pos = (Button) findViewById(R.id.muc_btn_map);
		btn_pos.setOnClickListener(this);

		btn_cd = (Button) findViewById(R.id.muc_btn_cd);
		btn_cd.setOnClickListener(this);

		list = (ListView) findViewById(R.id.muc_listView);

		gps_switch = (Switch) findViewById(R.id.muc_track_switch);
		gps_switch.setChecked(false);
		gps_switch.setOnCheckedChangeListener(this);
	}

	@Override
	public void onDestroy() {
		mServiceConnector.getService().getLocationProxy().stop();
		mServiceConnector
				.getService()
				.getIQProxy()
				.getProxy()
				.LeaveService(mServiceConnector.getService().getServiceJID(),
						onLeaveService);
		mServiceConnector.getService().unconnectMUC();
		mServiceConnector.unbindService();
		super.onDestroy();
	}

	@Override
	public void onClick(View view) {
		if (view != null)
			switch (view.getId()) {
			case R.id.muc_btn_send:
				sendMessage();
				break;
			case R.id.muc_btn_map:
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

	@Override
	public void onCheckedChanged(CompoundButton b, boolean checked) {
		if (b.getId() == R.id.muc_track_switch) {
			if (checked) {
				mServiceConnector.getService().getLocationProxy().start();
				mServiceConnector.getService().getClientData()
						.setPositionUpdateEnabled(true);
			} else {
				mServiceConnector.getService().getLocationProxy().stop();
				mServiceConnector.getService().getClientData()
						.setPositionUpdateEnabled(false);
			}
			MUCMessage msg = new MUCMessage();
			msg.setClientData(mServiceConnector.getService().getClientData());
			msg.setText("");
			mServiceConnector.getService().sendMUCMessage(msg);
		}
	}

	/*********************** class-spezific functions **********************/

	/**
	 * Send the MUCMessage to the MUC
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
				+ mServiceConnector.getService().getMucJID() + "%' AND "
				+ MessageItems.BODY + " NOT LIKE '%<text></text>%'";

		msgCursor = getContentResolver().query(MessageItems.contentUri, null,
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

	/** 
	 * custom CursorAdapter for displaying the muc-messages
	 */
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
