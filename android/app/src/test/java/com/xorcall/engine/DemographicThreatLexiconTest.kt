package com.xorcall.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemographicThreatLexiconTest {

    @Test
    fun testUserReportedDigitalArrestPhraseIsCritical() {
        val userPhrase = "Inspector from CBI Cyber Cell... arrest warrant... transfer 3 lakhs to RBI account"
        val result = DemographicThreatLexicon.evaluate(userPhrase)
        assertEquals(RiskLevel.CRITICAL, result.riskLevel)
        assertTrue(result.matchedKeywords.contains("Authority Impersonation"))
        assertTrue(result.matchedKeywords.contains("Coercive Payment Demand"))
    }

    @Test
    fun testDigitalArrestKeywordAloneIsCritical() {
        val daPhrase = "You are under digital arrest by Delhi Police."
        val result = DemographicThreatLexicon.evaluate(daPhrase)
        assertEquals(RiskLevel.CRITICAL, result.riskLevel)
    }

    @Test
    fun testSeniorDigitalArrestEscrowIsCritical() {
        val text = "Supreme Court digital arrest warrant issued! Mumbai Police CBI narcotics seized parcel. Liquidate fixed deposit and transfer ₹8 Lakhs to secret RBI escrow account now."
        val result = DemographicThreatLexicon.evaluate(text)
        assertEquals(RiskLevel.CRITICAL, result.riskLevel)
    }

    @Test
    fun testKinshipKidnapRansomIsCritical() {
        val text = "We have kidnapped your son. Pay 5 lakhs immediately or we will kill him."
        val result = DemographicThreatLexicon.evaluate(text)
        assertEquals(RiskLevel.CRITICAL, result.riskLevel)
    }

    @Test
    fun testStudentTaskScamIsCritical() {
        val text = "Telegram part-time review job: Like YouTube videos to earn ₹5,000. Balance frozen! Recharge crypto wallet with ₹40,000 USDT to release payout."
        val result = DemographicThreatLexicon.evaluate(text)
        assertEquals(RiskLevel.CRITICAL, result.riskLevel)
    }

    @Test
    fun testBenignHospitalDaughterConversationIsSafe() {
        val text = "I visited my daughter at the hospital, doctor says she needs rest. Please come to the hospital."
        val result = DemographicThreatLexicon.evaluate(text)
        assertEquals(RiskLevel.SAFE, result.riskLevel)
    }

    @Test
    fun testBenignCasualFamilyTalkIsSafe() {
        val text = "Hi daughter, let's have dinner tonight at the college campus restaurant."
        val result = DemographicThreatLexicon.evaluate(text)
        assertEquals(RiskLevel.SAFE, result.riskLevel)
    }
}
