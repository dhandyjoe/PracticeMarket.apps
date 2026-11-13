package com.automation.practice.feature.shop

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.automation.practice.MainActivity
import com.automation.practice.R
import com.automation.practice.app
import com.automation.practice.domain.Order
import com.automation.practice.domain.OrderStatus

/** Notifikasi sistem pesanan: dibuat (FR-PAYM-14) dan berganti status (FR-ORD-02). Ketuk membuka Detail Pesanan. */
object OrderNotifier {
    private const val CHANNEL = "orders"

    fun post(context: Context, order: Order) =
        notify(context, order, context.getString(R.string.notif_order_created_fmt, order.id))

    fun postStatus(context: Context, order: Order, status: OrderStatus) =
        notify(context, order, context.getString(R.string.notif_order_status_fmt, order.id, context.getString(status.label())))

    /** Notifikasi umum PracticePay (mis. penerima transfer simulasi, FR-PAY-07); tidak mengikuti saklar notifikasi pesanan. */
    fun postText(context: Context, key: String, text: String, deepLink: String) = show(context, key, text, deepLink)

    private fun notify(context: Context, order: Order, text: String) {
        if (!context.app.container.settings.prefs.value.orderNotifications) return
        show(context, order.id, text, "practiceapp://shop/order/${order.id}")
    }

    private fun show(context: Context, key: String, text: String, deepLink: String) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, context.getString(R.string.notif_channel_orders), NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
        val open = PendingIntent.getActivity(
            context, key.hashCode(),
            Intent(Intent.ACTION_VIEW, Uri.parse(deepLink), context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_bag)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify(key.hashCode(), notification)
    }
}
