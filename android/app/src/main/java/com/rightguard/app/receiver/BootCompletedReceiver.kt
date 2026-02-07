package com.rightguard.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // App is ready after boot — no persistent services to restart
            // Quick settings tile and widget will be available automatically
        }
    }
}
