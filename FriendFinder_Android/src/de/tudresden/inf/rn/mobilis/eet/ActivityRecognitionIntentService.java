package de.tudresden.inf.rn.mobilis.eet;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

public class ActivityRecognitionIntentService extends IntentService {

	private static final String TAG = "ActivityRecognitionIntentService";
	public static Handler onActivitiyResultHandler = null;

	public ActivityRecognitionIntentService() {
		super("ActivityRecognitionIntentService");
		// Log.v(TAG, "ActivityRecognitionIntentService.construct");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		// Log.v(TAG, "ActivityRecognitionIntentService.onHandleIntent, ");

		if (ActivityRecognitionResult.hasResult(intent)) {

			ActivityRecognitionResult result = ActivityRecognitionResult
					.extractResult(intent);

			DetectedActivity mostProbableActivity = result
					.getMostProbableActivity();

			int confidence = mostProbableActivity.getConfidence();
			int activityType = mostProbableActivity.getType();
			String activityName = getNameFromType(activityType);
			
			// Log.v(TAG, activityName + " (" + confidence + ")");

			if (ActivityRecognitionIntentService.onActivitiyResultHandler != null) {
				Bundle resultBundle = new Bundle();
				resultBundle.putString("activityName", activityName);
				resultBundle.putInt("activityConfidence", confidence);

				Message msg = new Message();
				msg.what = 1;
				msg.obj = resultBundle;
				ActivityRecognitionIntentService.onActivitiyResultHandler
						.sendMessage(msg);
			}
		}
	}

	private String getNameFromType(int activityType) {
		switch (activityType) {
		case DetectedActivity.IN_VEHICLE:
			return "in_vehicle";
		case DetectedActivity.ON_BICYCLE:
			return "on_bicycle";
		case DetectedActivity.ON_FOOT:
			return "on_foot";
		case DetectedActivity.STILL:
			return "still";
		case DetectedActivity.UNKNOWN:
			return "unknown";
		case DetectedActivity.TILTING:
			return "tilting";
		}
		return "unknown";
	}
}
