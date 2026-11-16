package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.domain.model.Assignment
import com.example.domain.model.PriorityType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReminderScheduler {
    const val CHANNEL_ID = "academico_priority_reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Lembretes Acadêmicos por Prioridade",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas de prazos e pontuação (Prioridade A: Nota, Prioridade B: Data de entrega)"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun scheduleReminder(context: Context, assignment: Assignment, courseName: String) {
        ensureChannel(context)
        if (!assignment.reminderEnabled || assignment.id.isBlank()) {
            cancelReminder(context, assignment.id)
            return
        }

        val triggerAt = assignment.dueDateMillis - (assignment.reminderHoursBefore * 3_600_000L)
        val now = System.currentTimeMillis()
        if (triggerAt <= now) {
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildPendingIntent(context, assignment, courseName)

        runCatching {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    fun triggerImmediatePreview(context: Context, assignment: Assignment, courseName: String) {
        val intent = buildReceiverIntent(context, assignment, courseName)
        context.sendBroadcast(intent)
    }

    fun cancelReminder(context: Context, assignmentId: String) {
        if (assignmentId.isBlank()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            assignmentId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pending != null) {
            alarmManager.cancel(pending)
            pending.cancel()
        }
    }

    private fun buildReceiverIntent(
        context: Context,
        assignment: Assignment,
        courseName: String
    ): Intent {
        val dateStr = SimpleDateFormat("dd/MM 'às' HH:mm", Locale("pt", "BR"))
            .format(Date(assignment.dueDateMillis))
        val isGradePriority = assignment.priorityType == PriorityType.GRADE_A.value
        val priorityTag = if (isGradePriority) {
            "Prioridade A (${formatScore(assignment.maxPoints)} pts)"
        } else {
            "Prioridade B (Entrega $dateStr)"
        }
        val attachmentNotice = if (assignment.requiresAttachment && assignment.aiVerdict != "approved") {
            " • Anexo obrigatório pendente de avaliação da IA."
        } else ""
        val body = "$courseName • Vale ${formatScore(assignment.maxPoints)} pts • Entrega: $dateStr$attachmentNotice"

        return Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_ASSIGNMENT_ID, assignment.id)
            putExtra(ReminderReceiver.EXTRA_TITLE, assignment.title)
            putExtra(ReminderReceiver.EXTRA_BODY, body)
            putExtra(ReminderReceiver.EXTRA_PRIORITY_TAG, priorityTag)
        }
    }

    private fun buildPendingIntent(
        context: Context,
        assignment: Assignment,
        courseName: String
    ): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            assignment.id.hashCode(),
            buildReceiverIntent(context, assignment, courseName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun formatScore(value: Double): String {
        return if (value % 1.0 == 0.0) value.toInt().toString()
        else String.format(Locale.US, "%.1f", value)
    }
}
