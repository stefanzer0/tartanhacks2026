import json
from pathlib import Path
from functools import lru_cache

from app.models.responses import (
    LegalRightsResponse,
    RightDetail,
    StatesResponse,
    StateInfo,
)


DATA_DIR = Path(__file__).resolve().parent.parent / "data"


@lru_cache()
def _load_legal_knowledge() -> dict:
    """Load and cache the legal knowledge JSON data."""
    knowledge_path = DATA_DIR / "legal_knowledge.json"
    with open(knowledge_path, "r", encoding="utf-8") as f:
        return json.load(f)


class LegalKnowledgeService:
    """Service for retrieving structured legal knowledge from the static data file.

    Provides access to state-specific legal rights, encounter-type-based guidance,
    and state metadata like recording consent laws.
    """

    def __init__(self):
        self.data = _load_legal_knowledge()

    def get_rights(self, state: str, encounter_type: str) -> LegalRightsResponse:
        """Get legal rights for a specific state and encounter type.

        Args:
            state: Two-letter US state code (e.g. 'PA').
            encounter_type: The encounter type key (e.g. 'TRAFFIC_STOP').

        Returns:
            LegalRightsResponse with structured rights data.

        Raises:
            KeyError: If the state or encounter type is not found.
        """
        states_data = self.data.get("states", {})
        state_upper = state.upper()

        if state_upper not in states_data:
            raise KeyError(f"State '{state_upper}' is not supported. Available states: {list(states_data.keys())}")

        state_data = states_data[state_upper]
        encounters = state_data.get("encounters", {})

        if encounter_type not in encounters:
            # Fall back to GENERAL if available
            if "GENERAL" in encounters:
                encounter_type = "GENERAL"
            else:
                raise KeyError(
                    f"Encounter type '{encounter_type}' not found for state '{state_upper}'. "
                    f"Available types: {list(encounters.keys())}"
                )

        encounter_data = encounters[encounter_type]

        rights = [
            RightDetail(
                right=r["right"],
                description=r["description"],
                applicable_to=r.get("applicable_to", ["ALL"]),
                source=r.get("source", ""),
            )
            for r in encounter_data.get("rights", [])
        ]

        state_specific_notes = state_data.get("state_specific_notes", [])

        return LegalRightsResponse(
            state=state_upper,
            encounter_type=encounter_type,
            rights=rights,
            state_specific_notes=state_specific_notes,
        )

    def get_states(self) -> StatesResponse:
        """Get all supported states with their metadata.

        Returns:
            StatesResponse containing a list of StateInfo objects.
        """
        states_data = self.data.get("states", {})
        state_list = []

        for code, info in states_data.items():
            state_list.append(
                StateInfo(
                    code=code,
                    name=info.get("name", code),
                    recording_consent=info.get("recording_consent", "unknown"),
                )
            )

        # Sort alphabetically by state code
        state_list.sort(key=lambda s: s.code)

        return StatesResponse(states=state_list)
