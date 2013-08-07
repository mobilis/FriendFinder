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
 * Dialog to set the servicename befor the create-service-iq was send
 */
public class NewServiceInstanceDialog extends DialogFragment{
	/**
	 * implemented by the MainActivity to get the data from the Dialog
	 *
	 */
	public interface NewServiceDialogListener{
		public void onDialogPositiveClick(NewServiceInstanceDialog dialog);
		public void onDialogNegativeClick(NewServiceInstanceDialog dialog);
	}
	
	private NewServiceDialogListener mListener;
	
	public String editNameString;	
	/**
	 * new service name
	 */
	public EditText editName;
	protected TextView serviceInfo;
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState){
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		LayoutInflater inflater = getActivity().getLayoutInflater(); 
		View view = inflater.inflate(R.layout.fragment_newservice_dialog, null);
		editName = (EditText) view.findViewById(R.id.newservice_name);
		editName.setText(editNameString);
		
		builder.setTitle("Create New Serviceinstance");
		builder.setView(view);
		
		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				mListener.onDialogPositiveClick(NewServiceInstanceDialog.this);
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mListener.onDialogNegativeClick(NewServiceInstanceDialog.this);
			}
		});
		Log.v(this.getTag(), "onCreateNewServiceDialog"); 
		return builder.create();
	}
	
	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		try{
			mListener = (NewServiceDialogListener) activity;
		} catch (ClassCastException e){
			throw new ClassCastException(activity.toString() + " must implement NewServiceDialogListener");
		}
	}
}
