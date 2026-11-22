package com.pbureera.ft_hangouts.ui

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
        val bubble = row.findViewById<LinearLayout>(R.id.message_bubble)
        val body = row.findViewById<TextView>(R.id.message_body)
        val time = row.findViewById<TextView>(R.id.message_time)

        val message = messages[position]
        body.text = message.body
        time.text = formatTime(row, message.timestamp)

        if (message.isSent) {
            row.gravity = Gravity.END
            bubble.setBackgroundResource(R.drawable.bubble_sent)
            body.setTextColor(row.context.getColor(R.color.bubble_sent_text))
            time.setTextColor(row.context.getColor(R.color.bubble_sent_time))
        } else {
            row.gravity = Gravity.START
            bubble.setBackgroundResource(R.drawable.bubble_received)
            body.setTextColor(row.context.getColor(R.color.bubble_received_text))
            time.setTextColor(row.context.getColor(R.color.bubble_received_time))
        }
        return row
    }

    /** "14:05" for today, "22 Nov, 14:05" for older messages, in the current language. */
    private fun formatTime(view: View, timestamp: Long): String {
        var flags = DateUtils.FORMAT_SHOW_TIME
        if (!DateUtils.isToday(timestamp)) flags = flags or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH
        return DateUtils.formatDateTime(view.context, timestamp, flags)
    }
}
