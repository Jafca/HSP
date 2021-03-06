package com.jafca.hsp

import android.content.Context
import android.os.AsyncTask
import android.preference.PreferenceManager
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class ApiDataRequest : AsyncTask<Any, String, String>() {
    var apiData: String? = null
    private lateinit var listener: MapsActivity.RunnableListener
    var url: String? = null

    override fun doInBackground(vararg objects: Any): String {
        listener = objects[0] as MapsActivity.RunnableListener
        url = objects[1] as String

        try {
            apiData = readUrl(url!!, objects[2] as Context)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return apiData!!
    }

    @Throws(IOException::class)
    fun readUrl(myUrl: String, context: Context): String {
        var data = ""
        var inputStream: InputStream? = null
        var urlConnection: HttpURLConnection? = null

        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                context.resources.getString(R.string.pref_sampleData),
                true
            )
        ) {
            try {
                inputStream = if (myUrl.startsWith("https://maps.googleapis.com/maps/api/place")) {
                    context.resources.openRawResource(R.raw.nearbysearch)
                } else {
                    context.resources.openRawResource(R.raw.directions)
                }
                data = inputStream.bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                Log.d("JSON ERROR", e.toString())
            }
        } else {
            try {
                val url = URL(myUrl)
                urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.connect()

                inputStream = urlConnection.inputStream
                data = inputStream.bufferedReader().use { it.readText() }
            } catch (e: MalformedURLException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                inputStream!!.close()
                urlConnection!!.disconnect()
            }
        }

        return data
    }

    override fun onPostExecute(result: String) {
        listener.onResult(result)
    }
}