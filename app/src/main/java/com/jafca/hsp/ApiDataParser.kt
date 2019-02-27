package com.jafca.hsp

import com.google.android.gms.maps.model.LatLng
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class ApiDataParser {
    internal fun parsePlaces(jsonData: String): List<HashMap<String, String>> {
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

    internal fun parseDirectionsDistance(jsonData: String): Float {
        val jsonResponse = JSONObject(jsonData)
        val routes = jsonResponse.getJSONArray("routes")
        val legs = routes.getJSONObject(0).getJSONArray("legs")
        var length = 0f
        for (i in 0 until legs.length()) {
            length += legs.getJSONObject(i).getJSONObject("distance").getString("value").toFloat()
        }
        return length
    }

    internal fun parseDirections(jsonData: String): MutableList<List<LatLng>> {
        val jsonResponse = JSONObject(jsonData)
        val routes = jsonResponse.getJSONArray("routes")
        val legs = routes.getJSONObject(0).getJSONArray("legs")
        val steps = legs.getJSONObject(0).getJSONArray("steps")
        val path: MutableList<List<LatLng>> = java.util.ArrayList()
        for (i in 0 until steps.length()) {
            val points = steps.getJSONObject(i).getJSONObject("polyline").getString("points")
            path.add(decodePoly(points))
        }
        return path
    }

    private fun decodePoly(encoded: String): List<LatLng> {
        val poly = java.util.ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = (if ((result and 1) != 0) (result shr 1).inv() else (result shr 1))
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = (if ((result and 1) != 0) (result shr 1).inv() else (result shr 1))
            lng += dlng
            val p = LatLng(
                ((lat.toDouble() / 1E5)),
                ((lng.toDouble() / 1E5))
            )
            poly.add(p)
        }
        return poly
    }
}