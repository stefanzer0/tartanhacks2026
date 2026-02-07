from fastapi import APIRouter, HTTPException, Depends

from app.models.requests import AdviceRequest
from app.models.responses import AdviceResponse, ErrorResponse
from app.services.gemini_service import GeminiService

router = APIRouter(
    prefix="/gemini",
    tags=["Gemini AI"],
)


def get_gemini_service() -> GeminiService:
    """Dependency injection for GeminiService."""
    return GeminiService()


@router.post(
    "/advice",
    response_model=AdviceResponse,
    summary="Get AI-powered legal advice",
    description=(
        "Analyzes the user's law enforcement encounter scenario and returns "
        "tailored legal advice, applicable rights, de-escalation tips, and "
        "relevant legal references."
    ),
    responses={
        400: {"model": ErrorResponse, "description": "Invalid request"},
        500: {"model": ErrorResponse, "description": "Internal server error"},
    },
)
async def get_advice(
    request: AdviceRequest,
    service: GeminiService = Depends(get_gemini_service),
) -> AdviceResponse:
    """Get AI-powered legal rights advice for a law enforcement encounter."""
    try:
        response = await service.get_advice(request)
        return response
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Failed to generate advice: {str(e)}",
        )
