package com.xorcall.engine

import org.junit.Assert.*
import org.junit.Test

class ContactWhitelistSyncManagerTest {

    @Test
    fun testOfficialEmergencyHelplineWhitelisting() {
        val emerg1930 = ContactWhitelistSyncManager.getEmergencyContacts().firstOrNull { it.number == "1930" }
        assertNotNull(emerg1930)
        assertTrue(emerg1930!!.isOfficialBankOrEmergency)
        assertTrue(emerg1930.isStarred)

        val emerg112 = ContactWhitelistSyncManager.getEmergencyContacts().firstOrNull { it.number == "112" }
        assertNotNull(emerg112)
        assertTrue(emerg112!!.isOfficialBankOrEmergency)
    }

    @Test
    fun testVerifiedBankHelplineWhitelisting() {
        val sbiTollFree = ContactWhitelistSyncManager.getBankContacts().firstOrNull { it.number == "1800112211" }
        assertNotNull(sbiTollFree)
        assertTrue(sbiTollFree!!.isOfficialBankOrEmergency)

        val hdfcTollFree = ContactWhitelistSyncManager.getBankContacts().firstOrNull { it.number == "18002026161" }
        assertNotNull(hdfcTollFree)
        assertTrue(hdfcTollFree!!.isOfficialBankOrEmergency)
    }

    @Test
    fun testPresetFamilyContactsCoverage() {
        val family = ContactWhitelistSyncManager.getPresetFamilyContacts()
        assertTrue(family.any { it.relationship == "Mother" && it.isStarred })
        assertTrue(family.any { it.relationship == "Father" && it.isStarred })
        assertTrue(family.any { it.relationship == "Spouse" && it.isStarred })
        assertTrue(family.any { it.relationship == "Son" })
        assertTrue(family.any { it.relationship == "Daughter" })
    }

    @Test
    fun testPhoneNumberNormalization() {
        assertEquals("9876543210", ContactWhitelistSyncManager.normalizePhoneNumber("+91 98765 43210"))
        assertEquals("9876543210", ContactWhitelistSyncManager.normalizePhoneNumber("+91-98765-43210"))
        assertEquals("9876543210", ContactWhitelistSyncManager.normalizePhoneNumber("(+91) 98765 43210"))
        assertEquals("1800112211", ContactWhitelistSyncManager.normalizePhoneNumber("1800-11-2211"))
    }
}
