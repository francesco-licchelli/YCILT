package com.example.ycilt.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.ycilt.R
import com.example.ycilt.utils.Workers.NOTIFICATION_CHANNEL_ID

class NotificationWorker(
	context: Context,
	workerParams: WorkerParameters
) : Worker(context, workerParams) {

	override fun doWork(): Result {
		val audioCount = inputData.getInt("audioCount", 1)
		val title = applicationContext.getString(R.string.notification_title)
		val message = applicationContext.getString(
			R.string.notification_message,
			audioCount,
			if (audioCount != 1) "s" else ""
		)

		sendNotification(title, message)
		return Result.success()
	}

	private fun sendNotification(title: String, message: String) {
		val notificationManager =
			applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

		val channelId = NOTIFICATION_CHANNEL_ID

		val channel = NotificationChannel(
			channelId,
			"Notifiche Worker",
			NotificationManager.IMPORTANCE_DEFAULT
		)
		notificationManager.createNotificationChannel(channel)

		val notification = NotificationCompat.Builder(applicationContext, channelId)
			.setContentTitle(title)
			.setContentText(message)
			.setSmallIcon(android.R.drawable.ic_dialog_info)
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
			.build()

		notificationManager.notify(1, notification)
	}

}
