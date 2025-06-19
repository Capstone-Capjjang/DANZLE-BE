# Service Class Descriptions

This section documents the main service classes in the `service` package. These classes implement the business logic that coordinates interactions between repositories, DTOs, and external services.

---

## AccuracySessionService

**File:** `AccuracySessionService.java`

**Description:**
Handles core logic related to pose accuracy evaluation sessions. Manages per-frame scoring, session creation, feedback generation, and summary responses.

**Responsibilities:**

* Create new `AccuracySession` entries
* Calculate average score
* Generate and attach GPT-based low score feedback
* Retrieve full or summary session responses

---

## ChallengeSessionService

**File:** `ChallengeSessionService.java`

**Description:**
Manages sessions where users perform under time-limited or segment-based challenge conditions.

**Responsibilities:**

* Create and store new challenge session records
* Retrieve challenge results and responses
* Validate challenge intervals and user participation

---

## PracticeSessionService

**File:** `PracticeSessionService.java`

**Description:**
Handles user practice session lifecycle, including creation, querying, and summary generation.

**Responsibilities:**

* Create new practice sessions
* Fetch sessions by user, song, or date
* Summarize recent or active practice activity

---

## SongService

**File:** `SongService.java`

**Description:**
Responsible for managing song-related queries and filtering. Provides access to audio/visual resources tied to songs.

**Responsibilities:**

* Retrieve songs by ID or metadata
* Filter or search by title, artist, or genre
* Return DTO-formatted song lists

---

## UserService

**File:** `UserService.java`

**Description:**
Encapsulates all user-related operations not directly tied to security. This includes registration, profile editing, and lookup.

**Responsibilities:**

* Create users (sign-up)
* Modify username, password, profile image
* Fetch user profile or public-facing user info

---

## VideoService

**File:** `VideoService.java`

**Description:**
Handles operations for saving, retrieving, and listing videos recorded during sessions.

**Responsibilities:**

* Link videos to appropriate session types (accuracy, practice, challenge)
* Retrieve videos for a user
* Serve DTO-based video metadata responses

---

## PasswordResetService

**File:** `PasswordResetService.java`

**Description:**
Manages email-based password reset flow. Issues verification codes, validates them, and updates passwords securely.

**Responsibilities:**

* Send reset code to email
* Verify code validity
* Save new password if verified

---

## OpenAiService

**File:** `OpenAiService.java`

**Description:**
Abstraction layer for interacting with the OpenAI API (e.g., GPT). Used for generating natural language feedback during pose evaluations.

**Responsibilities:**

* Construct and send prompt messages
* Parse GPT responses
* Fallback for safe failure handling (OpenAiApiException)

---

## CustomUserDetailsService

**File:** `CustomUserDetailsService.java`

**Description:**
Integrates the application's user data with Spring Security authentication flow by implementing `UserDetailsService`.

**Responsibilities:**

* Load user by username
* Wrap user data into `CustomUserDetails`
* Support login and security context setup

---

## CustomOAuth2UserService

**File:** `CustomOAuth2UserService.java`

**Description:**
Handles logic when a user authenticates through OAuth2 (e.g., Google). Maps OAuth2 user data to application-level users.

**Responsibilities:**

* Extract user info from OAuth2 provider
* Create new user if not found
* Return Spring Security compatible user details
