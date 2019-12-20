package com.chickenduy.locationApp.utility

import android.os.AsyncTask
import android.util.Log
import android.widget.Toast
import com.chickenduy.locationApp.MyApp
import me.pushy.sdk.Pushy
import java.net.URL

class RegisterForPushNotificationsAsync: AsyncTask<Void?, Void?, Exception?>() {
    override fun doInBackground(vararg p0: Void?): Exception? {
        try {
            // Assign a unique token to this device
            val deviceToken = Pushy.register(MyApp.instance)
            // Log it for debugging purposes
            Log.d("COMSERVICE", "Pushy device token: $deviceToken")
            // Send the token to your backend server via an HTTP GET request
            //URL("https://{YOUR_API_HOSTNAME}/register/device?token=$deviceToken").openConnection()
        } catch (exc: Exception) { // Return exc to onPostExecute
            return exc
        }
        // Success
        return null
    }

    override fun onPostExecute(exc: Exception?) { // Failed?
        if (exc != null) { // Show error as toast message
            Toast.makeText(
                MyApp.instance,
                exc.toString(),
                Toast.LENGTH_LONG
            ).show()
            return
        }
        // Succeeded, optionally do something to alert the user
    }
}