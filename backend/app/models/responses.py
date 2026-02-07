from pydantic import BaseModel, Field
from typing import Optional


class AdviceResponse(BaseModel):
    """Response from the AI-powered legal advice endpoint."""
    advice: str = Field(
        ...,
        description="The main legal advice text",
    )
    rights: list[str] = Field(
        default_factory=list,
        description="List of relevant constitutional/legal rights",
    )
    de_escalation_tips: list[str] = Field(
        default_factory=list,
        description="Practical tips for de-escalating the encounter",
    )
    legal_references: list[str] = Field(
        default_factory=list,
        description="Citations to relevant laws, court cases, or statutes",
    )


class RightDetail(BaseModel):
    """A single legal right with full detail."""
    right: str = Field(
        ...,
        description="Name or title of the right",
    )
    description: str = Field(
        ...,
        description="Detailed explanation of the right",
    )
    applicable_to: list[str] = Field(
        default_factory=list,
        description="Citizenship/status categories this right applies to",
    )
    source: str = Field(
        default="",
        description="Legal source or citation for this right",
    )


class LegalRightsResponse(BaseModel):
    """Response containing legal rights for a specific state and encounter type."""
    state: str = Field(
        ...,
        description="Two-letter US state code",
    )
    encounter_type: str = Field(
        ...,
        description="The type of law enforcement encounter",
    )
    rights: list[RightDetail] = Field(
        default_factory=list,
        description="List of applicable legal rights",
    )
    state_specific_notes: list[str] = Field(
        default_factory=list,
        description="Notes specific to this state's laws",
    )


class StateInfo(BaseModel):
    """Information about a US state's relevant laws."""
    code: str = Field(
        ...,
        description="Two-letter state code",
    )
    name: str = Field(
        ...,
        description="Full state name",
    )
    recording_consent: str = Field(
        ...,
        description="Recording consent law type: one-party | two-party | all-party",
    )


class StatesResponse(BaseModel):
    """Response containing available states and their key legal info."""
    states: list[StateInfo] = Field(
        default_factory=list,
        description="List of supported states with legal metadata",
    )


class ErrorResponse(BaseModel):
    """Standard error response."""
    detail: str = Field(
        ...,
        description="Error message",
    )
    error_code: Optional[str] = Field(
        default=None,
        description="Machine-readable error code",
    )
