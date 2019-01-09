package com.jafca.hsp

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class GetNearbyPlacesData : AsyncTask<Any, String, String>() {
    var googlePlacesData: String? = null
    private lateinit var listener: MapsActivity.RunnableListener
    var url: String? = null

    override fun doInBackground(vararg objects: Any): String {
        listener = objects[0] as MapsActivity.RunnableListener
        url = objects[1] as String

        try {
            googlePlacesData = readUrl(url!!, objects[2] as Context)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return googlePlacesData!!
    }

    @Throws(IOException::class)
    fun readUrl(myUrl: String, context: Context): String {
        var data = ""
        var inputStream: InputStream? = null
        var urlConnection: HttpURLConnection? = null

        if (BuildConfig.DEBUG) {
            try {
                inputStream = context.resources.openRawResource(R.raw.nearbysearch)
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
        val parseNearbyPlacesData = ParseNearbyPlacesData()
        val nearbyPlaceList: List<HashMap<String, String>> = parseNearbyPlacesData.parse(result)
        listener.onResult(nearbyPlaceList)
    }
}