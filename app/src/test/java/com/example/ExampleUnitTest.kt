package com.example

import com.example.model.DeviceDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testIqooHardwareDatabaseEntries() {
        val z7 = DeviceDatabase.KNOWN_DEVICES.find { it.marketingName == "iQOO Z7 5G" }
        assertNotNull(z7)
        assertEquals(4500, z7?.designCapacityMah)

        val z7Pro = DeviceDatabase.KNOWN_DEVICES.find { it.marketingName == "iQOO Z7 Pro 5G" }
        assertNotNull(z7Pro)
        assertEquals(4600, z7Pro?.designCapacityMah)

        val z9 = DeviceDatabase.KNOWN_DEVICES.find { it.marketingName == "iQOO Z9 5G" }
        assertNotNull(z9)
        assertEquals(5000, z9?.designCapacityMah)

        val z9x = DeviceDatabase.KNOWN_DEVICES.find { it.marketingName == "iQOO Z9x" }
        assertNotNull(z9x)
        assertEquals(6000, z9x?.designCapacityMah)

        val z9Pro = DeviceDatabase.KNOWN_DEVICES.find { it.marketingName == "iQOO Z9 Pro" }
        assertNotNull(z9Pro)
        assertEquals(5500, z9Pro?.designCapacityMah)

        val neo9Pro = DeviceDatabase.KNOWN_DEVICES.find { it.marketingName == "iQOO Neo 9 Pro" }
        assertNotNull(neo9Pro)
        assertEquals(5160, neo9Pro?.designCapacityMah)

        val iqoo12 = DeviceDatabase.KNOWN_DEVICES.find { it.marketingName == "iQOO 12" }
        assertNotNull(iqoo12)
        assertEquals(5000, iqoo12?.designCapacityMah)
    }
}
