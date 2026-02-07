import json
import logging

import google.generativeai as genai

from app.config import get_settings
from app.models.requests import AdviceRequest
from app.models.responses import AdviceResponse

settings = get_settings()
logger = logging.getLogger(__name__)


class GeminiService:
    """Service for interacting with the Google Gemini API to generate legal advice."""

    SYSTEM_PROMPT = """You are RightGuard, an AI legal rights advisor embedded in a citizen safety app. Your role is to help individuals understand and exercise their constitutional rights during law enforcement encounters.

CRITICAL RULES:
1. You are NOT a lawyer. Always note that users should consult a licensed attorney for case-specific legal advice.
2. NEVER encourage resisting, fleeing, or obstructing law enforcement.
3. Focus on: (a) applicable constitutional rights, (b) de-escalation, (c) what to say/not say, (d) staying safe.
4. Be concise — the user may be reading this on a phone screen during an active encounter.
5. Cite real case law and constitutional amendments when possible.
6. Tailor advice to the user's state, encounter type, and citizenship status.
7. If the user is undocumented or a non-citizen, include relevant immigration-specific rights (e.g., right to remain silent applies to everyone, right to refuse ICE entry without judicial warrant).

You MUST respond with valid JSON in exactly this format:
{
  "advice": "A concise 2-3 sentence recommendation of what to do or say right now.",
  "rights": ["Right 1 with legal basis", "Right 2 with legal basis", ...],
  "de_escalation_tips": ["Tip 1", "Tip 2", ...],
  "legal_references": ["Case or statute citation 1", "Case or statute citation 2", ...]
}

Provide 3-5 rights, 3-5 de-escalation tips, and 2-4 legal references. Keep each item to one sentence.
Respond ONLY with the JSON object, no markdown fences or extra text."""

    def __init__(self):
        self.api_key = settings.GEMINI_API_KEY
        self._model = None

    def _get_model(self):
        if self._model is None:
            genai.configure(api_key=self.api_key)
            self._model = genai.GenerativeModel(
                "gemini-2.5-flash",
                generation_config=genai.GenerationConfig(
                    temperature=0.3,
                    max_output_tokens=2048,
                    response_mime_type="application/json",
                ),
            )
        return self._model

    async def get_advice(self, request: AdviceRequest) -> AdviceResponse:
        if not self.api_key:
            logger.warning("No Gemini API key configured, using fallback")
            return self._get_fallback_advice(request)

        try:
            model = self._get_model()

            # Build conversation history for context
            history = []
            for msg in request.conversation_history:
                role = "model" if msg.role == "assistant" else msg.role
                history.append({"role": role, "parts": [msg.content]})

            # Build the user prompt with full context
            user_prompt = self._build_user_prompt(request)

            # Start chat with system instruction and history
            chat = model.start_chat(history=history)
            response = chat.send_message(
                f"{self.SYSTEM_PROMPT}\n\n{user_prompt}"
                if not history
                else user_prompt
            )

            # Parse the structured JSON response
            return self._parse_response(response.text)

        except json.JSONDecodeError as e:
            logger.error(f"Failed to parse Gemini response as JSON: {e}")
            return self._get_fallback_advice(request)
        except Exception as e:
            logger.error(f"Gemini API error: {e}")
            return self._get_fallback_advice(request)

    def _build_user_prompt(self, request: AdviceRequest) -> str:
        citizenship_info = ""
        if request.citizenship_status:
            status = request.citizenship_status.value.replace("_", " ").title()
            citizenship_info = f"\nCitizenship Status: {status}"
            if request.immigration_status:
                citizenship_info += f"\nImmigration Details: {request.immigration_status}"

        return (
            f"Encounter Type: {request.encounter_type.value.replace('_', ' ')}\n"
            f"State: {request.state.upper()}"
            f"{citizenship_info}\n\n"
            f"Situation: {request.situation_description}"
        )

    def _parse_response(self, text: str) -> AdviceResponse:
        # Strip markdown fences if present
        cleaned = text.strip()
        if cleaned.startswith("```"):
            cleaned = cleaned.split("\n", 1)[1] if "\n" in cleaned else cleaned[3:]
        if cleaned.endswith("```"):
            cleaned = cleaned[:-3]
        cleaned = cleaned.strip()

        data = json.loads(cleaned)
        return AdviceResponse(
            advice=data.get("advice", "Stay calm and exercise your rights."),
            rights=data.get("rights", []),
            de_escalation_tips=data.get("de_escalation_tips", []),
            legal_references=data.get("legal_references", []),
        )

    def _get_fallback_advice(self, request: AdviceRequest) -> AdviceResponse:
        """Fallback when Gemini is unavailable."""
        encounter_type = request.encounter_type.value
        state = request.state.upper()

        fallbacks = {
            "TRAFFIC_STOP": {
                "advice": (
                    f"During a traffic stop in {state}, remain calm and keep your hands "
                    "visible on the steering wheel. You must provide license, registration, "
                    "and insurance. You have the right to remain silent beyond that."
                ),
                "rights": [
                    "Right to remain silent (5th Amendment)",
                    "Right to refuse consent to a vehicle search (4th Amendment)",
                    "Right to record the encounter",
                    "Right to ask if you are free to leave",
                    "Right to an attorney if arrested",
                ],
                "de_escalation_tips": [
                    "Keep hands visible on the steering wheel",
                    "Inform the officer before reaching for documents",
                    "Speak calmly and politely",
                    "Do not argue — contest citations in court",
                ],
                "legal_references": [
                    "Terry v. Ohio, 392 U.S. 1 (1968)",
                    "Rodriguez v. United States, 575 U.S. 348 (2015)",
                    "Berkemer v. McCarty, 468 U.S. 420 (1984)",
                ],
            },
            "HOME_VISIT": {
                "advice": (
                    f"In {state}, you do NOT have to open your door without a warrant. "
                    "Ask to see it through a window. A valid warrant must be signed by a "
                    "judge and list the correct address."
                ),
                "rights": [
                    "Right to refuse entry without a warrant (4th Amendment)",
                    "Right to verify the warrant before opening the door",
                    "Right to remain silent",
                    "Right to refuse consent to a search",
                ],
                "de_escalation_tips": [
                    "Speak through a closed door when possible",
                    "Ask 'Do you have a warrant?' before opening",
                    "Do not physically block officers with a valid warrant",
                ],
                "legal_references": [
                    "Payton v. New York, 445 U.S. 573 (1980)",
                    "Kentucky v. King, 563 U.S. 452 (2011)",
                ],
            },
        }

        default = {
            "advice": (
                f"In {state}, stay calm and remember your constitutional rights. "
                "You have the right to remain silent and to an attorney. "
                "Do not consent to searches and do not physically resist."
            ),
            "rights": [
                "Right to remain silent (5th Amendment)",
                "Right against unreasonable searches (4th Amendment)",
                "Right to an attorney (6th Amendment)",
                "Right to record police in public",
            ],
            "de_escalation_tips": [
                "Stay calm and keep your hands visible",
                "Be polite but firm about your rights",
                "Do not argue or physically resist",
                "Document everything afterward",
            ],
            "legal_references": [
                "U.S. Constitution, 4th & 5th Amendments",
                "Miranda v. Arizona, 384 U.S. 436 (1966)",
            ],
        }

        data = fallbacks.get(encounter_type, default)
        return AdviceResponse(**data)
