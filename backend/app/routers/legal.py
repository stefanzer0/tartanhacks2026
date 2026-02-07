from fastapi import APIRouter, HTTPException, Query, Depends

from app.models.requests import EncounterType
from app.models.responses import LegalRightsResponse, StatesResponse, ErrorResponse
from app.services.legal_knowledge_service import LegalKnowledgeService

router = APIRouter(
    prefix="/legal",
    tags=["Legal Knowledge"],
)


def get_legal_service() -> LegalKnowledgeService:
    """Dependency injection for LegalKnowledgeService."""
    return LegalKnowledgeService()


@router.get(
    "/rights",
    response_model=LegalRightsResponse,
    summary="Get legal rights by state and encounter type",
    description=(
        "Returns a structured list of legal rights applicable to a specific "
        "type of law enforcement encounter in a given US state."
    ),
    responses={
        404: {"model": ErrorResponse, "description": "State or encounter type not found"},
    },
)
async def get_rights(
    state: str = Query(
        ...,
        min_length=2,
        max_length=2,
        description="Two-letter US state code (e.g. PA, CA, NY)",
    ),
    encounter_type: EncounterType = Query(
        ...,
        description="The type of law enforcement encounter",
    ),
    service: LegalKnowledgeService = Depends(get_legal_service),
) -> LegalRightsResponse:
    """Retrieve legal rights for a given state and encounter type."""
    try:
        response = service.get_rights(state=state.upper(), encounter_type=encounter_type.value)
        return response
    except KeyError as e:
        raise HTTPException(status_code=404, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get(
    "/states",
    response_model=StatesResponse,
    summary="List all supported states",
    description="Returns a list of all US states supported by the system with key legal metadata.",
)
async def get_states(
    service: LegalKnowledgeService = Depends(get_legal_service),
) -> StatesResponse:
    """List all supported states with their legal metadata."""
    try:
        response = service.get_states()
        return response
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
