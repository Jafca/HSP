package com.jafca.hsp

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DataParseUnitTests {
    @Test
    fun parsePlaceData() {
        val data = "{\n" +
                "  \"html_attributions\": [],\n" +
                "  \"next_page_token\": \"CpQECAIAAApdnSmo\",\n" +
                "  \"results\": [\n" +
                "    {\n" +
                "      \"geometry\": {\n" +
                "        \"location\": {\n" +
                "          \"lat\": 53.741356,\n" +
                "          \"lng\": -0.3332544\n" +
                "        },\n" +
                "        \"viewport\": {\n" +
                "          \"northeast\": {\n" +
                "            \"lat\": 53.74270498029149,\n" +
                "            \"lng\": -0.331905419708498\n" +
                "          },\n" +
                "          \"southwest\": {\n" +
                "            \"lat\": 53.7400070197085,\n" +
                "            \"lng\": -0.3346033802915021\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      \"icon\": \"https://maps.gstatic.com/mapfiles/place_api/icons/generic_business-71.png\",\n" +
                "      \"id\": \"e27bee3852c50fc0511bfae7dbed0a0f9b0552f2\",\n" +
                "      \"name\": \"APCOA King William House Car Park\",\n" +
                "      \"opening_hours\": {\n" +
                "        \"open_now\": true\n" +
                "      },\n" +
                "      \"photos\": [\n" +
                "        {\n" +
                "          \"height\": 959,\n" +
                "          \"html_attributions\": [\n" +
                "            \"<a href=\\\"https://maps.google.com/maps/contrib/107467299233097487771/photos\\\">Sunny Costin</a>\"\n" +
                "          ],\n" +
                "          \"photo_reference\": \"CmRaAAAARdubB8sl1Nxu1o41jbDHHu5OXnX1inyTpvOh70aLtKBvZGYH5\",\n" +
                "          \"width\": 1280\n" +
                "        }\n" +
                "      ],\n" +
                "      \"place_id\": \"ChIJBW2iCCK-eEgRCcovwuVgW_o\",\n" +
                "      \"plus_code\": {\n" +
                "        \"compound_code\": \"PMR8+GM Hull, United Kingdom\",\n" +
                "        \"global_code\": \"9C5XPMR8+GM\"\n" +
                "      },\n" +
                "      \"rating\": 3.3,\n" +
                "      \"reference\": \"ChIJBW2iCCK-eEgRCcovwuVgW_o\",\n" +
                "      \"scope\": \"GOOGLE\",\n" +
                "      \"types\": [\n" +
                "        \"parking\",\n" +
                "        \"point_of_interest\",\n" +
                "        \"establishment\"\n" +
                "      ],\n" +
                "      \"vicinity\": \"King William House, Lowgate, Hull\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"geometry\": {\n" +
                "        \"location\": {\n" +
                "          \"lat\": 53.7484591,\n" +
                "          \"lng\": -0.3455031\n" +
                "        },\n" +
                "        \"viewport\": {\n" +
                "          \"northeast\": {\n" +
                "            \"lat\": 53.74975948029149,\n" +
                "            \"lng\": -0.344127769708498\n" +
                "          },\n" +
                "          \"southwest\": {\n" +
                "            \"lat\": 53.7470615197085,\n" +
                "            \"lng\": -0.346825730291502\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      \"icon\": \"https://maps.gstatic.com/mapfiles/place_api/icons/generic_business-71.png\",\n" +
                "      \"id\": \"0a68f15278954fb4af076259665778a016e65500\",\n" +
                "      \"place_id\": \"ChIJ2b7_EjC-eEgRUBBP_E7Esrw\",\n" +
                "      \"plus_code\": {\n" +
                "        \"compound_code\": \"PMX3+9Q Hull, United Kingdom\",\n" +
                "        \"global_code\": \"9C5XPMX3+9Q\"\n" +
                "      },\n" +
                "      \"reference\": \"ChIJ2b7_EjC-eEgRUBBP_E7Esrw\",\n" +
                "      \"scope\": \"GOOGLE\",\n" +
                "      \"types\": [\n" +
                "        \"parking\",\n" +
                "        \"point_of_interest\",\n" +
                "        \"establishment\"\n" +
                "      ],\n" +
                "      \"vicinity\": \"8 Pryme Street, Hull\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"geometry\": {\n" +
                "        \"location\": {\n" +
                "          \"lat\": 53.7468388,\n" +
                "          \"lng\": -0.3494753999999999\n" +
                "        },\n" +
                "        \"viewport\": {\n" +
                "          \"northeast\": {\n" +
                "            \"lat\": 53.7481905802915,\n" +
                "            \"lng\": -0.3481362697084979\n" +
                "          },\n" +
                "          \"southwest\": {\n" +
                "            \"lat\": 53.7454926197085,\n" +
                "            \"lng\": -0.350834230291502\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      \"icon\": \"https://maps.gstatic.com/mapfiles/place_api/icons/generic_business-71.png\",\n" +
                "      \"id\": \"1b3ab86161737b0a2bbdef44ad13c367bd5dc630\",\n" +
                "      \"name\": \"St Stephens Square Car Park\",\n" +
                "      \"opening_hours\": {\n" +
                "        \"open_now\": true\n" +
                "      },\n" +
                "      \"photos\": [\n" +
                "        {\n" +
                "          \"height\": 3120,\n" +
                "          \"html_attributions\": [\n" +
                "            \"<a href=\\\"https://maps.google.com/maps/contrib/101811209502343497039/photos\\\">Daniel Bertman</a>\"\n" +
                "          ],\n" +
                "          \"photo_reference\": \"CmRaAAAA2hNMY\",\n" +
                "          \"width\": 4160\n" +
                "        }\n" +
                "      ],\n" +
                "      \"place_id\": \"ChIJK07eGC6-eEgRUS3w1fHrlrQ\",\n" +
                "      \"plus_code\": {\n" +
                "        \"compound_code\": \"PMW2+P6 Hull, United Kingdom\",\n" +
                "        \"global_code\": \"9C5XPMW2+P6\"\n" +
                "      },\n" +
                "      \"rating\": 4.2,\n" +
                "      \"reference\": \"ChIJK07eGC6-eEgRUS3w1fHrlrQ\",\n" +
                "      \"scope\": \"GOOGLE\",\n" +
                "      \"types\": [\n" +
                "        \"parking\",\n" +
                "        \"point_of_interest\",\n" +
                "        \"establishment\"\n" +
                "      ]\n" +
                "    }\n" +
                "  ],\n" +
                "  \"status\": \"OK\"\n" +
                "}"

        val parseNearbyPlacesData = ParseNearbyPlacesData()
        val result = parseNearbyPlacesData.parse(data)

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
}