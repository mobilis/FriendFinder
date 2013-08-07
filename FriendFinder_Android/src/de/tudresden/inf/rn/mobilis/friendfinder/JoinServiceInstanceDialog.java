package de.tudresden.inf.rn.mobilis.friendfinder;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

/**
 * dialog to show some informations and set the username befor enter the service-instance
 *
 */
public class JoinServiceInstanceDialog extends DialogFragment{
	/**
	 * implemented by the MainActivity to get the data from the Dialog
	 *
	 */
	public interface SettingsDialogListener{
		public void onDialogPositiveClick(JoinServiceInstanceDialog dialog);
		public void onDialogNegativeClick(JoinServiceInstanceDialog dialog);
	}
	
	private SettingsDialogListener mListener;
	
	public String serviceInfoString;
	public String editNameString;
	/**
	 * username
	 */
	public EditText editName;
	protected TextView serviceInfo;
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState){
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		LayoutInflater inflater = getActivity().getLayoutInflater(); 
		View view = inflater.inflate(R.layout.fragment_settings_dialog, null);
		editName = (EditText) view.findViewById(R.id.settings_name);
		editName.setText(editNameString);
		serviceInfo = (TextView) view.findViewById(R.id.settings_service_info);
		serviceInfo.setText(serviceInfoString);
		
		builder.setTitle("Join Service Instance");
		builder.setView(view);
		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				mListener.onDialogPositiveClick(JoinServiceInstanceDialog.this);
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mListener.onDialogNegativeClick(JoinServiceInstanceDialog.this);
			}
		});
		Log.v(this.getTag(), "onCreateDialog"); 
		return builder.create();
	}
	
	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		try{
			mListener = (SettingsDialogListener) activity;
		} catch (ClassCastException e){
			throw new ClassCastException(activity.toString() + " must implement SettingsDialogListener");
		}
	}
}
