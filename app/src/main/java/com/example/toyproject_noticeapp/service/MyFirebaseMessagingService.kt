// app/src/main/java/com/example/toyproject_noticeapp/service/MyFirebaseMessagingService.kt

package com.example.toyproject_noticeapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log // ❗️ 로그 확인을 위해 추가
import androidx.core.app.NotificationCompat
import com.example.toyproject_noticeapp.MainActivity
import com.example.toyproject_noticeapp.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.concurrent.atomic.AtomicInteger

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // ❗️ onMessageReceived 함수를 아래 코드로 수정합니다.
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "FCM Message ID: ${remoteMessage.messageId}")
        Log.d(TAG, "FCM Notification Title: ${remoteMessage.notification?.title}")
        Log.d(TAG, "FCM Notification Body: ${remoteMessage.notification?.body}")

        // ❗️ notification 페이로드가 있을 때만 sendNotification 호출
        // ❗️ 이렇게 하면 앱이 포그라운드/백그라운드 상태 모두에서 알림을 표시할 수 있습니다.
        remoteMessage.notification?.let {
            sendNotification(it.title ?: "새로운 공지", it.body ?: "내용 없음")
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
    }

    private fun sendNotification(title: String, body: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "new_notice_channel"
        val channelName = "신규 공지 알림"
        val notificationGroupId = "com.example.toyproject_noticeapp.NEW_NOTICE_GROUP"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // ❗️ 아이콘을 mipmap/ic_launcher로 변경
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setGroup(notificationGroupId)

        val summaryNotification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Hanshin Now!")
            .setContentText("새로운 공지가 도착했습니다.")
            .setSmallIcon(R.mipmap.ic_launcher) // ❗️ 아이콘을 mipmap/ic_launcher로 변경
            .setStyle(NotificationCompat.InboxStyle().setSummaryText("새로운 공지"))
            .setGroup(notificationGroupId)
            .setGroupSummary(true)
            .build()

        notificationManager.notify(NotificationID.incrementAndGet(), notificationBuilder.build())
        notificationManager.notify(SUMMARY_ID, summaryNotification)
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService" // ❗️ 로그 태그 추가
        private const val SUMMARY_ID = 0
        private val NotificationID = AtomicInteger(1)
    }
}