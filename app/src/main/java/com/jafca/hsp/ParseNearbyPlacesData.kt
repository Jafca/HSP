package com.jafca.hsp

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class ParseNearbyPlacesData {
    internal fun parse(jsonData: String): List<HashMap<String, String>> {
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