package com.b866f.launcher

import android.content.Intent
import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    val launchIntent: Intent
)
