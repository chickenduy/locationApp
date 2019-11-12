package com.chickenduy.locationApp.backgroundServices

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.chickenduy.locationApp.MyApp
import com.chickenduy.locationApp.ui.MainActivity
import kotlin.system.exitProcess

class MyExceptionHandler(
    private val activity: Activity
): Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        val intent = Intent(activity, MainActivity::class.java)
        intent.putExtra("crash", true)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent = PendingIntent.getActivity(MyApp.instance.baseContext, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        val mgr = MyApp.instance.baseContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pendingIntent)
        activity.finish()
        exitProcess(2)
    }
}