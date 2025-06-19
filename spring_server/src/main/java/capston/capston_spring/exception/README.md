# Exception Class Descriptions

This section documents custom exceptions located in the `exception` package. These exceptions are used throughout the system for handling domain-specific error conditions.

---

## OpenAiApiException

**File:** `OpenAiApiException.java`

**Description:**
Thrown when a request to the OpenAI API fails or returns an unexpected result. Typically used to wrap errors related to GPT feedback generation.

---

## SessionNotFoundException

**File:** `SessionNotFoundException.java`

**Description:**
Thrown when a referenced session (such as `AccuracySession`, `PracticeSession`, or `ChallengeSession`) cannot be found in the database.

---

## SongNotFoundException

**File:** `SongNotFoundException.java`

**Description:**
Raised when a song ID is provided but no matching song entity exists in the system.

---

## UserNotFoundException

**File:** `UserNotFoundException.java`

**Description:**
Raised when a user-related lookup (e.g., by username or ID) fails to locate a valid `AppUser` entity.

---

## VideoNotFoundException

**File:** `VideoNotFoundException.java`

**Description:**
Thrown when a `RecordedVideo` entity cannot be found using the provided identifier or criteria.
