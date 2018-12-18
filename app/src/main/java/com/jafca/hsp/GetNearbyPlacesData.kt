package com.jafca.hsp

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
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
        val nearbyPlaceList: List<HashMap<String, String>> = parse(result)
        listener.onResult(nearbyPlaceList)
    }

    private fun parse(jsonData: String): List<HashMap<String, String>> {
        var jsonArray: JSONArray? = null
        val jsonObject: JSONObject?

        try {
            jsonObject = JSONObject(jsonData)
            jsonArray = jsonObject.getJSONArray("results")
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return getPlaces(jsonArray!!)
    }

    private fun getPlaces(jsonArray: JSONArray): List<HashMap<String, String>> {

        val count: Int = jsonArray.length()
        val placesList = ArrayList<HashMap<String, String>>()
        var placeMap: HashMap<String, String>

        for (i in 0 until count) {
            try {
                placeMap = getPlace(jsonArray.get(i) as JSONObject)
                placesList.add(placeMap)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        return placesList
    }

    private fun getPlace(googlePlaceJson: JSONObject): HashMap<String, String> {
        val googlePlacesMap = HashMap<String, String>()

        try {
            googlePlacesMap["place_name"] =
                    if (googlePlaceJson.isNull("name"))
                        "--NA--"
                    else
                        googlePlaceJson.getString("name")

            googlePlacesMap["vicinity"] =
                    if (googlePlaceJson.isNull("vicinity"))
                        "--NA--"
                    else
                        googlePlaceJson.getString("vicinity")

            googlePlacesMap["lat"] =
                    googlePlaceJson.getJSONObject("geometry").getJSONObject("location").getString("lat")
            googlePlacesMap["lng"] =
                    googlePlaceJson.getJSONObject("geometry").getJSONObject("location").getString("lng")
            googlePlacesMap["reference"] = googlePlaceJson.getString("reference")
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return googlePlacesMap
    }
}