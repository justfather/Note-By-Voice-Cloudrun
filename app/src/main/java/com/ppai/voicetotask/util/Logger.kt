package com.ppai.voicetotask.util

import android.util.Log
import com.ppai.voicetotask.BuildConfig

object Logger {
    private const val TAG = "VoiceToTask"
    
    fun d(message: String, tag: String = TAG) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }
    
    fun i(message: String, tag: String = TAG) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, message)
        }
    }
    
    fun w(message: String, tag: String = TAG) {
        Log.w(tag, message)
    }
    
    fun e(message: String, throwable: Throwable? = null, tag: String = TAG) {
        Log.e(tag, message, throwable)
    }
    
    fun v(message: String, tag: String = TAG) {
        if (BuildConfig.DEBUG) {
            Log.v(tag, message)
        }
    }
}