package com.mapapp.utils

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.IntentSender.SendIntentException
import android.location.LocationManager
import android.util.Log
import com.mapapp.R
import android.widget.Toast
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.mapapp.constants.AppConstants
import com.mapapp.constants.AppConstants.FASTEST_INTERVAL
import com.mapapp.constants.AppConstants.GPS_REQUEST
import com.mapapp.constants.AppConstants.UPDATE_INTERVAL

class GpsUtils(private val context: Context) {
    private val mSettingsClient: SettingsClient = LocationServices.getSettingsClient(context)
    private val mLocationSettingsRequest: LocationSettingsRequest
    private val locationManager: LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val locationRequest: LocationRequest = LocationRequest.create()

    init {
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = UPDATE_INTERVAL
        locationRequest.fastestInterval = FASTEST_INTERVAL
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        mLocationSettingsRequest = builder.build()
        //**************************
        builder.setAlwaysShow(true) //this is the key ingredient
        //**************************
    } // method for turn on GPS

    fun turnGPSOn(onGpsListener: OnGpsListener?) {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            //if gps is already turned on, send back true response
            onGpsListener?.gpsStatus(true)
        } else {
            //else, attempt to turn it on
            mSettingsClient
                .checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(
                    (context as Activity)
                ) {
                    //  GPS is already enable, callback GPS status through listener
                    onGpsListener?.gpsStatus(true)
                }
                .addOnFailureListener(
                    context
                ) { e ->
                    val statusCode =
                        (e as ApiException).statusCode
                    when (statusCode) {
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                            // Show the dialog by calling startResolutionForResult(), and check the
                            // result in onActivityResult().
                            val rae = e as ResolvableApiException
                            rae.startResolutionForResult(
                                context,
                                GPS_REQUEST
                            )
                        } catch (sie: SendIntentException) {
                            Log.i(TAG, "PendingIntent unable to execute request.")
                        }
                        LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                            val errorMessage =
                                context.getString(R.string.inadequate_location_settings)
                            Log.e(TAG, errorMessage)
                            Toast.makeText(
                                context,
                                errorMessage,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
        }
    }

    interface OnGpsListener {
        fun gpsStatus(isGPSEnable: Boolean)
    }

}