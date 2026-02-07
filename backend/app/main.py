import logging

from fastapi import FastAPI
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from starlette.requests import Request

from app.config import get_settings
from app.routers import gemini, legal

logger = logging.getLogger(__name__)
settings = get_settings()

app = FastAPI(
    title="RightGuard API",
    description=(
        "AI-powered legal rights advisor that helps individuals understand "
        "their constitutional rights during law enforcement encounters."
    ),
    version="1.0.0",
    docs_url="/docs" if settings.DEBUG else None,
    redoc_url="/redoc" if settings.DEBUG else None,
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    body = await request.body()
    print(f"\n=== VALIDATION ERROR ===")
    print(f"URL: {request.url}")
    print(f"Request body: {body.decode()}")
    print(f"Errors: {exc.errors()}")
    print(f"========================\n")
    return JSONResponse(
        status_code=422,
        content={"detail": exc.errors()},
    )


@app.get("/health", tags=["Health"])
async def health_check():
    """Health check endpoint to verify the API is running."""
    return {
        "status": "healthy",
        "version": "1.0.0",
        "environment": settings.APP_ENV,
    }


# Include routers
app.include_router(gemini.router, prefix=settings.API_V1_PREFIX)
app.include_router(legal.router, prefix=settings.API_V1_PREFIX)
