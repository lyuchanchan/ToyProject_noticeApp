// app/src/main/java/com/example/toyproject_noticeapp/service/MyFirebaseMessagingService.kt

package com.example.toyproject_noticeapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.toyproject_noticeapp.MainActivity
import com.example.toyproject_noticeapp.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.concurrent.atomic.AtomicInteger


class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "FCM Message ID: ${remoteMessage.messageId}")
        Log.d(TAG, "FCM Notification Title: ${remoteMessage.notification?.title}")
        Log.d(TAG, "FCM Notification Body: ${remoteMessage.notification?.body}")

        remoteMessage.notification?.let {
            sendNotification(it.title ?: "새로운 공지", it.body ?: "내용 없음")
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
        // 👇 *** 여기가 핵심 수정 사항입니다! ***
        sendRegistrationToServer(token)
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
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setGroup(notificationGroupId)

        val summaryNotification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Hanshin Now!")
            .setContentText("새로운 공지가 도착했습니다.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setStyle(NotificationCompat.InboxStyle().setSummaryText("새로운 공지"))
            .setGroup(notificationGroupId)
            .setGroupSummary(true)
            .build()

        notificationManager.notify(NotificationID.incrementAndGet(), notificationBuilder.build())
        notificationManager.notify(SUMMARY_ID, summaryNotification)
    }

    private fun sendRegistrationToServer(token: String?) {
        val uid = Firebase.auth.currentUser?.uid
        if (uid != null && token != null) {
            val userDocRef = Firebase.firestore.collection("users").document(uid)
            userDocRef.update("fcmToken", token)
                .addOnSuccessListener { Log.d(TAG, "FCM token updated successfully.") }
                .addOnFailureListener { e -> Log.w(TAG, "Error updating FCM token", e) }
        }
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
        private const val SUMMARY_ID = 0
        private val NotificationID = AtomicInteger(1)
    }
}