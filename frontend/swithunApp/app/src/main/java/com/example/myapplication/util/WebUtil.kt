package com.example.myapplication.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.myapplication.SwithunLog
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONObject
import ru.gildor.coroutines.okhttp.await

val TAG = "swithun-xxxx 「WebUtil    」"

fun postRequest(my_url: String, params: Map<String, String>): JSONObject? {
    /** 创建HttpClient  */
    val httpClient = OkHttpClient.Builder().build()

    val formBody: RequestBody = FormBody.Builder().apply {
        for ((key, value) in params) {
            add(key, value)
        }
    }.build()

    val requestBody = Request.Builder()
        .url(my_url)
        .post(formBody)
        .build()

    val response = httpClient.newCall(requestBody).execute()

    return response.body?.string()?.let {
        JSONObject(it)
    }
}

@SuppressLint("LongLogTag")
suspend fun getRequest(
    my_url: String,
    urlEncodeParams: UrlEncodeParams? = null,
    headerParams: HeaderParams? = null,
): JSONObject? {
    val response = getRequestWithOriginalResponse(my_url, urlEncodeParams, headerParams) ?: return null
    return response.getRequestBodyJsonObject()
}

suspend fun getRequestWithOriginalResponse(
    my_url: String,
    urlEncodeParams: UrlEncodeParams? = null,
    headerParams: HeaderParams? = null,
): Response? {
    val httpClient = OkHttpClient.Builder().build()

    val httpBuilder: HttpUrl.Builder = my_url.toHttpUrlOrNull()?.newBuilder() ?: return null
    if (urlEncodeParams != null) {
        for ((key, value) in urlEncodeParams.params) {
            httpBuilder.addQueryParameter(key, value)
        }
    }

    val request = Request.Builder()
        .url(httpBuilder.build())
        .apply {
            if (headerParams != null) {
                val headers = Headers.Builder()
                for ((key, value) in headerParams.params) {
                    headers.add(key, value)
                }
                headers(headers.build())
            }
        }
        .build()
    return httpClient.newCall(request).await()

}

@SuppressLint("LongLogTag")
fun Response.getRequestBodyJsonObject(): JSONObject? {
    return this.body?.string()?.let {
        Log.d(TAG, it)
        JSONObject(it)
    }
}

@SuppressLint("LongLogTag")
fun Response.getRequestCookies(): List<String> {
    return this.headers.values("Set-Cookie").also {
        for (cookie in it) {
            Log.d(TAG, "cookie: $cookie")
        }
    }
}

class UrlEncodeParams {
    var params = mutableMapOf<String, String>()
        private set

    fun put(key: String, value: String) {
        params[key] = value
    }
}

class HeaderParams {
    var params = mutableMapOf<String, String>()
        private set

    fun setBilibiliCookie(activity: Activity?) {

        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: let {
            SwithunLog.d("get sharedPref failed")
            return
        }

        val cookieSessionData = sharedPref.getString("SESSDATA", "")

        if (cookieSessionData.isNullOrBlank()) {
            SwithunLog.d("cookieSessionData: $cookieSessionData")
        } else {
            SwithunLog.d("cookieSessionData.isNullOrBlank()")
            params["Cookie"] = "SESSDATA=$cookieSessionData"
        }

    }
}