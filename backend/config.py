import os
from pydantic import BaseModel

class Settings(BaseModel):
    PROJECT_NAME: str = "XORcall Cyber Safety Brain"
    VERSION: str = "1.0.0"
    HOST: str = os.getenv("HOST", "0.0.0.0")
    PORT: int = int(os.getenv("PORT", "8000"))
    GROQ_API_KEY: str = os.getenv("GROQ_API_KEY", "")
    GROQ_MODEL: str = os.getenv("GROQ_MODEL", "llama-3.3-70b-versatile")
    EMERGENCY_COOLDOWN_SECS: int = 45
    PASSIVE_HEARTBEAT_WINDOW_MINS: int = 15

settings = Settings()
