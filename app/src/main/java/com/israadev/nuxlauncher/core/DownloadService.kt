package com.israadev.nuxlauncher.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.israadev.nuxlauncher.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Layanan latar dengan notifikasi progres: menjaga proses tetap hidup selama [DownloadController] mengunduh
 * (layar padam atau aplikasi ditinggal tidak mematikan unduhan 500 MiB). Berhenti sendiri saat unduhan selesai,
 * gagal, atau dibatalkan. Unduhannya sendiri tetap dijalankan [DownloadController]; layanan ini hanya penjaga.
 */
class DownloadService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Game file downloads", NotificationManager.IMPORTANCE_LOW))
        }
        val first = build(null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, first, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, first)
        }
        scope.launch {
            DownloadController.state.collect { s ->
                if (s is DownloadController.State.Running) {
                    nm.notify(NOTIFICATION_ID, build(s.progress))
                } else {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun build(p: GameFiles.Progress?): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val b = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading game files")
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
        if (p == null || p.preparing || p.bytesTotal <= 0L) {
            b.setProgress(0, 0, true)
        } else {
            b.setProgress(100, (p.fraction * 100).toInt(), false)
                .setContentText("${p.bytesDone / (1024L * 1024L)} / ${p.bytesTotal / (1024L * 1024L)} MB")
        }
        return b.build()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val CHANNEL = "downloads"
        const val NOTIFICATION_ID = 4101
    }
}
