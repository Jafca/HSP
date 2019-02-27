package com.jafca.hsp

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DataParseUnitTests {
    @Test
    fun parsePlaceData() {
        val classLoader = javaClass.classLoader
        val resource = classLoader!!.getResourceAsStream("samplePlacesData.json")
        val data = resource.bufferedReader().use { (it.readText()) }

        val apiDataParser = ApiDataParser()
        val result = apiDataParser.parsePlaces(data)

        assertEquals("APCOA King William House Car Park", result[0]["place_name"])
        assertEquals("King William House, Lowgate, Hull", result[0]["vicinity"])
        assertEquals("53.741356", result[0]["lat"])
        assertEquals("-0.3332544", result[0]["lng"])

        // No Name
        assertEquals("--NA--", result[1]["place_name"])
        assertEquals("8 Pryme Street, Hull", result[1]["vicinity"])

        // No Vicinity
        assertEquals("St Stephens Square Car Park", result[2]["place_name"])
        assertEquals("--NA--", result[2]["vicinity"])
    }

    @Test
    fun parseDirectionDataDistance() {
        val classLoader = javaClass.classLoader
        val resource = classLoader!!.getResourceAsStream("sampleDirectionsData.json")
        val data = resource.bufferedReader().use { (it.readText()) }

        val apiDataParser = ApiDataParser()
        val result = apiDataParser.parseDirectionsDistance(data)

        assertEquals(275f, result)
    }

    @Test
    fun parseDirectionData() {
        val classLoader = javaClass.classLoader
        val resource = classLoader!!.getResourceAsStream("sampleDirectionsData.json")
        val data = resource.bufferedReader().use { (it.readText()) }

        val apiDataParser = ApiDataParser()
        val result = apiDataParser.parseDirections(data)

        assertEquals(4, result.size)
    }
}