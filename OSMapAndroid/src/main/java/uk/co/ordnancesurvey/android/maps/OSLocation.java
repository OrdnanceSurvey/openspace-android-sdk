/**
 * OpenSpace Android SDK Licence Terms
 *
 * The OpenSpace Android SDK is protected by © Crown copyright – Ordnance Survey 2013.[https://github.com/OrdnanceSurvey]
 *
 * All rights reserved (subject to the BSD licence terms as follows):.
 *
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * Neither the name of Ordnance Survey nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 *
 */
package uk.co.ordnancesurvey.android.maps;

import java.util.List;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Toast;

final class OSLocation implements SensorEventListener, LocationSource {
	private final Context mContext;
	private boolean mShowSettingsDialog;
	private final static String PREFS_NAME = "osmaps_preferences";
	private final static String SETTINGS_PREF = "show_settings_dialog";
	private boolean mLastWasGPSandNetwork;
	OnLocationChangedListener mListener;
	private Location mLocation = null;
	private boolean mIsCheckingLocation;
	private SensorManager mSensorManager;
	private float[] mGravity;
	private float[] mGeo; 


	public OSLocation(Context context) 
	{
		mContext = context;
		
		// Restore preferences
		SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
	    mShowSettingsDialog =  settings.getBoolean(SETTINGS_PREF, true);
	}
	
	private static final String TAG = "OSLocation";
	private LocationListener mLocationListener = new LocationListener() {
		public void onLocationChanged(Location location) {
			float bearing = 0f;
			if(mLocation != null)
			{
				bearing = mLocation.getBearing();
			}			
			mLocation = location;
			mLocation.setBearing(bearing);
			mListener.onLocationChanged(location);
		}
		public void onProviderDisabled(String provider) {
			// Show settings dialog?
		}
		public void onProviderEnabled(String provider) {
		}
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	};
	
	public boolean isCheckingLocation() {
		return mIsCheckingLocation;
	}
	
	public void activate(OnLocationChangedListener listener) {
		if(mIsCheckingLocation)
		{
			return;
		}
		mListener = listener;
		final LocationManager lm = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
		mLocation = null;
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.NO_REQUIREMENT);
		criteria.setAltitudeRequired(false);
		criteria.setBearingRequired(false);
		criteria.setCostAllowed(false);
		criteria.setSpeedRequired(false);
		
		boolean gps_enabled = false;
		boolean network_enabled = false;
		
		
		try 
		{
			gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
    		Log.v(TAG, "GPS enabled: " + gps_enabled);
		}	
		catch(Exception ex){}
    	try
    	{
    		network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    		Log.v(TAG, "Network enabled: " + network_enabled);
    	} catch(Exception ex){}

    	if(!gps_enabled && !network_enabled)
		{
			Toast.makeText(mContext,  "Please enable a My Location source in system settings", Toast.LENGTH_LONG).show();
			// No point requesting updates from a non-existent provider.
			return;
		}
		
		lm.requestLocationUpdates(1,1, criteria, mLocationListener, null);
		mIsCheckingLocation = true;

		// Tell the user if they can improve accuracy if we were previous using GPS and Network, and now one of these
		// has become unavailable.
    	if((!gps_enabled || !network_enabled) && mLastWasGPSandNetwork)
		{               
			if(mShowSettingsDialog)
			{
				Dialog dialog = createDialog();
				dialog.show();
			}
		} 
    	mLastWasGPSandNetwork = gps_enabled & network_enabled;
    	
	    mSensorManager = (SensorManager)mContext.getSystemService(Context.SENSOR_SERVICE);
	    Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
	    if(sensor != null)
	    {
	    	mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
	    }
	    sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	    if(sensor != null)
	    {
	    	mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
	    }
	}
	
	public void deactivate() {
		if(!mIsCheckingLocation)
		{
			return;
		}
		final LocationManager lm = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
		lm.removeUpdates(mLocationListener);
		mSensorManager.unregisterListener(this);
		synchronized(this)
		{
			mListener = null;
		}
		mIsCheckingLocation = false;
	}
	
	// Sensor stuff
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		final float rad2deg = (float)(180.0f/Math.PI);  
		float rValues[] = new float[9];

		float orientation[] = new float[3];
		if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
		{
			mGravity = new float[3];
			System.arraycopy(event.values, 0, mGravity, 0, 3);
		}
		else if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
		{
			mGeo = new float[3];
			System.arraycopy(event.values, 0, mGeo, 0, 3);
		}
		if(mGravity== null || mGeo == null)
		{
			return;
		}
		if(!SensorManager.getRotationMatrix(rValues, null, mGravity, mGeo))
		{
			return;
		}
		SensorManager.getOrientation(rValues, orientation);
		float bearing = orientation[0] * rad2deg;
		if(mLocation == null)
		{
			// New location with null provider. This will have no accuracy (0.0) by default.
			// All locations generated by the location manager have an accuracy
			mLocation = new Location("");
		}
		mLocation.setBearing(bearing);
		if(mLocation.getAccuracy() > 0.0)
		{
			synchronized(this)
			{
				if(mListener != null)
				{
					mListener.onLocationChanged(mLocation);
				}
			}
		}
	}

	@TargetApi(11)
	private LinearLayout createDialogLinearLayoutAPI11() {
		return new LinearLayout(mContext, null, android.R.style.Theme_Dialog);
	}

	private LinearLayout createDialogLinearLayout() {
		if (Build.VERSION.SDK_INT >= 11) {
			return createDialogLinearLayoutAPI11();
		}
		return new LinearLayout(mContext, null);
	}
	
	public Dialog createDialog() {
			LinearLayout layout = createDialogLinearLayout();
			layout.setOrientation(LinearLayout.HORIZONTAL);
		    LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
		            LinearLayout.LayoutParams.MATCH_PARENT,
		            LinearLayout.LayoutParams.MATCH_PARENT);
		    layout.setLayoutParams(llp);
		    layout.setPadding(10,10,10,10);
		    final CheckBox cb = new CheckBox(mContext);
		    cb.setText("Never show this again");
		    
		    cb.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		    layout.addView(cb);
	 
	        // Use the Builder class for convenient dialog construction
	        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
	        builder.setView(layout)
	               .setTitle("Improve location accuracy")
	               .setMessage("To enhance your Maps experience:\n\n• Turn on GPS and mobile network location")
	               .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
	                   public void onClick(DialogInterface dialog, int id) {
	                       if (cb.isChecked()) {
	                           doNotShowAgain();
	                       }
	                       Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
	                       mContext.startActivity(myIntent);
	                   }
	               })
	               .setNegativeButton("Skip", new DialogInterface.OnClickListener() {
	                   public void onClick(DialogInterface dialog, int id) {
	                       // User cancelled the dialog
	                       if (cb.isChecked()) {
	                           doNotShowAgain();
	                       }
	                   }
	               });
	        // Create the AlertDialog object and return it
	        return builder.create();
	    }
	     
	    private void doNotShowAgain() {
	    	mShowSettingsDialog = false;
			// Restore preferences
			SharedPreferences settings = mContext.getSharedPreferences(PREFS_NAME, 0);
			SharedPreferences.Editor editor = settings.edit();
		    editor.putBoolean(SETTINGS_PREF, false);
		    editor.commit();
	    }

}
