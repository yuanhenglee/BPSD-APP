package com.dilab.bpsd_warning.log

import org.json.JSONObject
import java.text.SimpleDateFormat

class LogItem( var title: String, var message: String, var time: String) {

    constructor(title: String, message: String) : this(
        title,
        message,
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(System.currentTimeMillis()))

    constructor(logJson: JSONObject) : this(
        logJson.getString("title"),
        logJson.getString("message"),
        logJson.getString("time"))

    fun toJsonString(): String {
        return "{\"title\":\"$title\",\"message\":\"$message\",\"time\":\"$time\"}"
    }

    fun toJson(): JSONObject {
        return JSONObject(toJsonString())
    }
}