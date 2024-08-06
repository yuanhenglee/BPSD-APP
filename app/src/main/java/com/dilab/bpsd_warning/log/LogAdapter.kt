package com.dilab.bpsd_warning.log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dilab.bpsd_warning.R
import java.util.LinkedList

class LogAdapter(var logList: LinkedList<LogItem>) : RecyclerView.Adapter<LogAdapter.LogViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(logList[position])
    }

    override fun getItemCount(): Int = logList.size

    class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.logTitleTextView)
        private val bodyTextView: TextView = itemView.findViewById(R.id.logBodyTextView)
        private val timeTextView: TextView = itemView.findViewById(R.id.logTimeTextView)

        fun bind(logItem: LogItem) {
            titleTextView.text = logItem.title
            bodyTextView.text = logItem.message
            timeTextView.text = logItem.time
        }
    }
}