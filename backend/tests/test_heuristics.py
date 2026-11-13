import pytest
from detector.rule_heuristics import CallHeuristicSession

def test_digital_arrest_kill_chain():
    session = CallHeuristicSession("test_da_01")
    
    # 1. Authority claim
    r1 = session.feed("Hello, I am Inspector Phansalkar from the Mumbai Crime Branch.")
    assert "authority_claim" in r1["flags"]
    assert r1["level"] in ["SAFE", "WATCH"]

    # 2. Legal threat
    r2 = session.feed("An arrest warrant is issued in your name for money laundering.")
    assert "legal_threat" in r2["flags"]

    # 3. Isolation gag order
    r3 = session.feed("You must stay on the video call and not tell your family. It is confidential.")
    assert "isolation" in r3["flags"]
    assert r3["risk_score"] >= 0.40
    assert r3["level"] == "WATCH"

    # 4. Urgent payment demand -> Trigger Kill-Chain
    r4 = session.feed("Send 5 lakh rupees by RTGS to the safe RBI account right now.")
    assert "payment_demand" in r4["flags"]
    assert r4["risk_score"] >= 0.90
    assert r4["level"] == "CRITICAL"
    assert r4["threat_type"] == "DIGITAL_ARREST_SCAM"
    assert r4["demographic_target"] == "SENIOR_CITIZEN"
    assert "STAGE_3_ISOLATION_CONFINEMENT" in r4["kill_chain_stages_triggered"]

def test_kinship_deepfake_extortion():
    session = CallHeuristicSession("test_kin_01")
    
    # 1. Kinship hook + accident
    r1 = session.feed("Papa, I got into a fatal accident! Police arrested me, please help me.")
    assert "kinship_emergency" in r1["flags"]

    # 2. Immediate ransom/bail payment
    r2 = session.feed("Transfer 50000 rupees to this UPI ID right now or they will lock me in jail.")
    assert "payment_demand" in r2["flags"]
    assert r2["risk_score"] >= 0.90
    assert r2["level"] == "CRITICAL"
    assert r2["threat_type"] == "KINSHIP_DEEPFAKE_EXTORTION"
    assert r2["demographic_target"] == "PARENT"

def test_student_telegram_task_scam():
    session = CallHeuristicSession("test_student_01")
    
    # 1. Telegram task / part-time job offer
    r1 = session.feed("Earn 5000 daily with part-time job! Simple telegram task to review hotels and youtube like.")
    assert "task_job_scam" in r1["flags"]
    assert r1["demographic_target"] == "STUDENT_YOUTH"

    # 2. Demanding prepaid recharge to withdraw balance
    r2 = session.feed("You completed level 3, now deposit 25000 rupees task deposit to unlock your crypto wallet withdraw.")
    assert "payment_demand" in r2["flags"]
    assert r2["risk_score"] >= 0.90
    assert r2["level"] == "CRITICAL"
    assert r2["threat_type"] == "STUDENT_TASK_SCAM"
    assert r2["demographic_target"] == "STUDENT_YOUTH"

def test_student_college_fee_phishing():
    session = CallHeuristicSession("test_college_01")
    
    # Pretext: BITS Pilani college fee or seat cancellation
    r1 = session.feed("This is registrar office, your bits pilani fee portal shows seat cancellation risk.")
    assert "college_fee_phishing" in r1["flags"]

    # Demand: transfer immediately
    r2 = session.feed("Pay 120000 rupees directly to registrar upi right now or hall ticket hold.")
    assert "payment_demand" in r2["flags"]
    assert r2["risk_score"] >= 0.90
    assert r2["level"] == "CRITICAL"
    assert r2["threat_type"] == "COLLEGE_FEE_PHISHING"
    assert r2["demographic_target"] == "STUDENT_YOUTH"

def test_corporate_ceo_wire_scam():
    session = CallHeuristicSession("test_corp_01")
    
    # Pretext: CEO in board meeting whaling
    r1 = session.feed("I am in a board meeting do not call my number. Need an urgent wire transfer for confidential acquisition.")
    assert "corporate_wire_scam" in r1["flags"]
    assert r1["demographic_target"] == "WORKING_PROFESSIONAL"

    # Demand: Transfer funds immediately
    r2 = session.feed("Transfer 8 lakh rupees to vendor payment account immediately.")
    assert "payment_demand" in r2["flags"]
    assert r2["risk_score"] >= 0.90
    assert r2["level"] == "CRITICAL"
    assert r2["threat_type"] == "CEO_IMPERSONATION_FRAUD"
    assert r2["demographic_target"] == "WORKING_PROFESSIONAL"

def test_genuine_hospital_call_mitigation():
    session = CallHeuristicSession("test_med_01")
    
    # Legitimate doctor asking parents to visit hospital with zero payment demand
    r = session.feed("Hello, this is Dr. Mehta from Apollo Hospital. Your son was admitted to the emergency ward. Please come to the hospital immediately.")
    assert "physical_hospital_presence" in r["flags"]
    assert r["risk_score"] <= 0.30
    assert r["level"] == "SAFE"
    assert r["threat_type"] == "GENUINE_MEDICAL_TRIAGE"

def test_routine_workplace_mitigator():
    session = CallHeuristicSession("test_work_01")
    
    # Developer conversation with Jira / PR
    r = session.feed("Can you review pull request on github and check jira ticket before the daily standup?")
    assert "routine_workplace_mitigator" in r["flags"]
    assert r["risk_score"] <= 0.20
    assert r["level"] == "SAFE"
    assert r["threat_type"] == "BENIGN_WORK_OR_CAMPUS"
