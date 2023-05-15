package it.matteoleggio.gallerydl.receiver

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import it.matteoleggio.gallerydl.R
import it.matteoleggio.gallerydl.util.FileUtil
import it.matteoleggio.gallerydl.util.NotificationUtil
import it.matteoleggio.gallerydl.util.UiUtil

class SharedDownloadNotificationReceiver : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.blank)
        val intent = intent
        val message = intent.getStringExtra("share")
        val path = intent.getStringExtra("path")
        val notificationId = intent.getIntExtra("notificationID", 0)
        if (message != null && path != null) {
            if (notificationId != 0) NotificationUtil(this).cancelDownloadNotification(notificationId)
            val uiUtil = UiUtil(FileUtil())
            uiUtil.shareFileIntent(this, listOf(path))
            this.finishAffinity()
        }
    }
}