package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.DeviceDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("iQOO Battery Health Inspector", appName)
    }

    @Test
    fun `device database contains required iqoo models`() {
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
    }

    @Test
    fun `override design capacity saves and restores correctly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        DeviceDatabase.saveManualOverrideCapacity(context, 5800)
        val (cap, isOverridden) = DeviceDatabase.getEffectiveDesignCapacity(context)
        assertEquals(5800, cap)
        assertTrue(isOverridden)

        DeviceDatabase.saveManualOverrideCapacity(context, null)
    }
}
