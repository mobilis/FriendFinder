package de.tudresden.inf.rn.mobilis.friendfinder.service;

import java.util.ArrayList;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

/**
 * connect the BackgroundService to the Activities
 * look in Android-Documentation for additional details
 */
public class ServiceConnector {

	private static final String TAG = "ServiceConnector";

	private Context mContext;
	private Boolean mIsServiceBound = false;

	private BackgroundService mService = null;

	private ArrayList<Handler> onServiceBoundHandlers;

	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder binder) {
			Log.v(TAG, "onServiceConnected(), " + onServiceBoundHandlers.size() + " Handlers registered");
			mService = ((BackgroundService.LocalBinder) binder).getService();

			// notify all registered handlers
			for(Handler h:onServiceBoundHandlers){
				h.sendEmptyMessage(0);
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.v(TAG, "onServiceDisconnected()");
			mService = null;
		}
	};

	public ServiceConnector(Context context) {
		// if (context == null)
		// throw new Exception("parameter context must not null");
		mContext = context;
		onServiceBoundHandlers = new ArrayList<Handler>();
	}

	public void bindService() {
		this.bindService(null);
	}

	public void bindService(Handler h) {
		if(!mIsServiceBound){
			if (h != null)
				onServiceBoundHandlers.add(h);
			Intent i = new Intent(mContext, BackgroundService.class);
			mContext.bindService(i, mConnection, Context.BIND_AUTO_CREATE);
			mIsServiceBound = true;
		}
		else{
			h.sendEmptyMessage(0);
		}
	}

	public void unbindService() {
		if (mIsServiceBound) {
			mContext.unbindService(mConnection);
			mIsServiceBound = false;
		}
	}

	public Boolean isServiceBound() {
		return mIsServiceBound;
	}

	public BackgroundService getService() {
		return mService;
	}
}
