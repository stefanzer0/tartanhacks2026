from pydantic import BaseModel, Field
from typing import Optional
from enum import Enum


class EncounterType(str, Enum):
    """Types of law enforcement encounters."""
    TRAFFIC_STOP = "TRAFFIC_STOP"
    PEDESTRIAN_STOP = "PEDESTRIAN_STOP"
    HOME_VISIT = "HOME_VISIT"
    PROTEST = "PROTEST"
    IMMIGRATION_CHECK = "IMMIGRATION_CHECK"
    GENERAL = "GENERAL"


class CitizenshipStatus(str, Enum):
    """Citizenship status categories."""
    US_CITIZEN = "US_CITIZEN"
    PERMANENT_RESIDENT = "PERMANENT_RESIDENT"
    VISA_HOLDER = "VISA_HOLDER"
    UNDOCUMENTED = "UNDOCUMENTED"
    PREFER_NOT_TO_SAY = "PREFER_NOT_TO_SAY"


class ConversationMessage(BaseModel):
    """A single message in the conversation history."""
    role: str = Field(
        ...,
        description="Role of the message sender: 'user' or 'assistant'",
        pattern="^(user|assistant)$",
    )
    content: str = Field(
        ...,
        description="Content of the message",
        min_length=1,
    )


class AdviceRequest(BaseModel):
    """Request body for the legal advice endpoint."""
    encounter_type: EncounterType = Field(
        ...,
        description="The type of law enforcement encounter",
    )
    state: str = Field(
        ...,
        description="Two-letter US state code (e.g. PA, CA, NY)",
        min_length=2,
        max_length=2,
    )
    citizenship_status: Optional[CitizenshipStatus] = Field(
        default=CitizenshipStatus.PREFER_NOT_TO_SAY,
        description="The user's citizenship/immigration status",
    )
    immigration_status: Optional[str] = Field(
        default=None,
        description="Additional immigration status details if applicable",
    )
    situation_description: str = Field(
        ...,
        description="Free-text description of the current situation",
        min_length=1,
        max_length=5000,
    )
    conversation_history: list[ConversationMessage] = Field(
        default_factory=list,
        description="Previous conversation messages for context continuity",
    )
