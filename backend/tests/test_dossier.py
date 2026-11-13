from services.dossier_generator import DossierGenerator

def test_dossier_pdf_generation():
    generator = DossierGenerator()
    data = {
        "session_id": "TEST_SESSION_88",
        "threat_type": "DIGITAL_ARREST_SCAM",
        "risk_score": 0.96,
        "caller_phone": "+91-98765-43210",
        "harvested_iocs": {
            "upi_ids": ["sbi-safe@okaxis"],
            "bank_accounts": ["501004918291"]
        },
        "transcript_lines": [
            "00:10 - 'CBI officer here.'",
            "00:30 - 'Transfer money to safe account.'"
        ]
    }
    pdf_bytes = generator.generate_pdf_bytes(data)
    assert len(pdf_bytes) > 1000
    assert pdf_bytes.startswith(b"%PDF")
