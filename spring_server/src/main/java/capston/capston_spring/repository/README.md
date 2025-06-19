# Repository Interface Descriptions

This section describes interfaces in the `repository` package, which extend Spring Data JPA’s `JpaRepository` to provide data access operations for various entities.

---

## AccuracyFrameEvaluationRepository

**File:** `AccuracyFrameEvaluationRepository.java`

**Description:**
Provides CRUD operations for the `AccuracyFrameEvaluation` entity. Allows querying frame-level accuracy evaluations.

**Key Usage:**

* Retrieve all frame evaluations for a given session
* Save or update per-frame evaluation results

---

## AccuracySessionRepository

**File:** `AccuracySessionRepository.java`

**Description:**
Handles persistence for the `AccuracySession` entity, which represents user sessions for accuracy evaluation.

**Custom Methods (if present):**

* Find sessions by user, date, or mode
* Fetch sessions with sorting or filtering criteria

---

## ChallengeSessionRepository

**File:** `ChallengeSessionRepository.java`

**Description:**
Provides data access for `ChallengeSession`, which tracks challenge-mode attempts by users.

**Typical Operations:**

* Find challenge sessions by user or song
* Save challenge session results

---

## PracticeSessionRepository

**File:** `PracticeSessionRepository.java`

**Description:**
Used to persist and query `PracticeSession` entities, which are created during free practice without accuracy scoring.

**Example Use Cases:**

* Get all practice sessions of a user
* Filter by session date or mode

---

## RecordedVideoRepository

**File:** `RecordedVideoRepository.java`

**Description:**
Manages `RecordedVideo` entities, which contain metadata and references to recorded session videos.

**Query Scenarios:**

* Find videos by user or session ID
* Retrieve videos by mode (practice, accuracy, challenge)

---

## SongRepository

**File:** `SongRepository.java`

**Description:**
Access layer for managing `Song` entities. Used to retrieve and filter song metadata and file paths.

**Expected Queries:**

* Find songs by title, artist, or genre
* Retrieve available song list for practice or challenge

---

## UserRepository

**File:** `UserRepository.java`

**Description:**
Provides user-related database operations for the `AppUser` entity. Supports user authentication and registration workflows.

**Typical Methods:**

* Find by username or email
* Check for user existence
* Save new users during signup
