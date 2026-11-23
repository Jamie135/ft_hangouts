package com.pbureera.ft_hangouts.ui

import android.content.Context
import android.text.format.DateUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.TextView
import com.pbureera.ft_hangouts.R
import com.pbureera.ft_hangouts.data.Message
import java.util.Calendar

/**
 * Rows of the conversation screen: messages we sent are bubbles on the right,
 * messages the contact sent us are bubbles on the left.
 */
class MessageAdapter(private val inflater: LayoutInflater) : BaseAdapter() {

    private var messages: List<Message> = emptyList()

    fun submit(newMessages: List<Message>) {
        messages = newMessages
        notifyDataSetChanged()
    }

    override fun getCount() = messages.size
    override fun getItem(position: Int): Message = messages[position]
    override fun getItemId(position: Int) = messages[position].id
    override fun hasStableIds() = true

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val row = (convertView ?: inflater.inflate(R.layout.item_message, parent, false)) as LinearLayout
        val day = row.findViewById<TextView>(R.id.message_day)
        val bubble = row.findViewById<LinearLayout>(R.id.message_bubble)
        val body = row.findViewById<TextView>(R.id.message_body)
        val time = row.findViewById<TextView>(R.id.message_time)
        val context = row.context

        val message = messages[position]
        val previous = messages.getOrNull(position - 1)
        body.text = message.body
        time.text = DateUtils.formatDateTime(context, message.timestamp, DateUtils.FORMAT_SHOW_TIME)

        // The day ("Today", "Yesterday", "Mon 22 Nov") above the first message of each day.
        val newDay = previous == null || !sameDay(previous.timestamp, message.timestamp)
        day.visibility = if (newDay) View.VISIBLE else View.GONE
        if (newDay) day.text = formatDay(context, message.timestamp)

        // Messages that follow each other from the same person are grouped close together.
        val grouped = !newDay && previous?.isSent == message.isSent &&
            message.timestamp - previous.timestamp < GROUP_WINDOW_MS
        val density = context.resources.displayMetrics.density
        row.setPadding(row.paddingLeft, ((if (grouped) 2 else 8) * density).toInt(), row.paddingRight, 0)

        if (message.isSent) {
            row.gravity = Gravity.END
            bubble.setBackgroundResource(R.drawable.bubble_sent)
            body.setTextColor(context.getColor(R.color.bubble_sent_text))
            time.setTextColor(context.getColor(R.color.bubble_sent_time))
        } else {
            row.gravity = Gravity.START
            bubble.setBackgroundResource(R.drawable.bubble_received)
            body.setTextColor(context.getColor(R.color.bubble_received_text))
            time.setTextColor(context.getColor(R.color.bubble_received_time))
        }
        return row
    }

    private fun sameDay(a: Long, b: Long): Boolean {
        val ca = Calendar.getInstance().apply { timeInMillis = a }
        val cb = Calendar.getInstance().apply { timeInMillis = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
            ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }

    /** "Today", "Yesterday", else the date in the current language (with the year if not this year). */
    private fun formatDay(context: Context, timestamp: Long): String {
        val now = System.currentTimeMillis()
        return when {
            sameDay(timestamp, now) -> context.getString(R.string.day_today)
            sameDay(timestamp, now - DateUtils.DAY_IN_MILLIS) -> context.getString(R.string.day_yesterday)
            else -> DateUtils.formatDateTime(
                context, timestamp,
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or
                    DateUtils.FORMAT_ABBREV_WEEKDAY or DateUtils.FORMAT_ABBREV_MONTH,
            )
        }
    }

    private companion object {
        /** Messages less than 2 minutes apart from the same person are drawn as a group. */
        const val GROUP_WINDOW_MS = 2 * 60 * 1000L
    }
}
