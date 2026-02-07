package com.rightguard.app.domain.model

enum class EncounterType {
    TRAFFIC_STOP,
    PEDESTRIAN_STOP,
    HOME_VISIT,
    PROTEST,
    IMMIGRATION_CHECK,
    GENERAL
}

enum class CitizenshipStatus {
    US_CITIZEN,
    PERMANENT_RESIDENT,
    VISA_HOLDER,
    UNDOCUMENTED,
    PREFER_NOT_TO_SAY
}

enum class ImmigrationStatus {
    NOT_APPLICABLE,
    VALID_VISA,
    EXPIRED_VISA,
    ASYLUM_SEEKER,
    DACA,
    TPS,
    PREFER_NOT_TO_SAY
}

enum class IncidentStatus {
    ACTIVE,
    ENDED,
    ARCHIVED
}

enum class RecordingStatus {
    RECORDING,
    STOPPED,
    ENCRYPTED,
    FAILED
}

enum class AppMode {
    IDLE,
    SAFEGUARD,
    PANIC
}
