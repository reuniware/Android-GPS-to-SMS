package com.reuniware.gpstosms

import android.content.Context
import android.preference.PreferenceManager
import com.google.android.gms.maps.model.LatLng

class Utils {
    companion object {
        fun storeLastKnownLocation(applicationContext: Context, latLng: LatLng) {
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            val editor = sharedPref.edit()
            editor.putString("lastKnownLatitude", latLng.latitude.toString())
            editor.putString("lastKnownLongitude", latLng.longitude.toString())
            editor.apply()
        }

        fun getLastKnownLocation(applicationContext: Context): LatLng {
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            return when (sharedPref.contains("lastKnownLatitude") && sharedPref.contains("lastKnownLongitude")){
                true -> {
                    val latitude = sharedPref.getString("lastKnownLatitude", "").toDouble()
                    val longitude= sharedPref.getString("lastKnownLongitude", "").toDouble()
                    LatLng(latitude, longitude)
                }
                false -> {
                    LatLng(0.0,0.0)
                }
            }
        }

        fun storeLastKnownLocationDate(applicationContext: Context, strDate: String)
        {
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            val editor = sharedPref.edit()
            editor.putString("lastKnownLocationDate", strDate)
            editor.apply()
        }

        fun getLastKnownLocationDate(applicationContext: Context):String {
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            return sharedPref.getString("lastKnownLocationDate", "")
        }

        fun storeLastKnownAddress(applicationContext: Context, strAddress: String)
        {
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            val editor = sharedPref.edit()
            editor.putString("lastKnownAddress", strAddress)
            editor.apply()
        }

        fun getLastKnownAddress(applicationContext: Context) : String {
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            return sharedPref.getString("lastKnownAddress", "")
        }

        fun storeSmsRecipient(applicationContext: Context, phone : String) {
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            val editor = sharedPref.edit()
            editor.putString("smsRecipient", phone)
            editor.apply()
        }

        /*fun resetSmsRecipient(applicationContext: Context) {
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            val editor = sharedPref.edit()
            editor.putString("smsRecipient", "")
            editor.apply()
        }*/

        fun resetAllData(applicationContext: Context) {
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            val editor = sharedPref.edit()
            editor.clear().commit()
        }

        fun getSmsRecipient(applicationContext: Context): String {
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            return sharedPref.getString("smsRecipient", "")
        }

    }
}