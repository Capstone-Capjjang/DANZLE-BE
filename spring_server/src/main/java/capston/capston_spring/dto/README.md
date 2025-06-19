# DTO Class Descriptions

This section documents classes in the `dto` package. These are Data Transfer Objects used to encapsulate and transport data between layers, especially from controller to client or between services.

---

## AccuracySessionResponse

**File:** `AccuracySessionResponse.java`

**Description:**
Represents the full response structure for a completed accuracy session. Contains session metadata, detailed score information, and additional evaluation context.

**Fields May Include:**

* Session ID
* Start & end timestamps
* Mode (e.g., full/highlight)
* Average score
* List of per-frame evaluations

---

## AccuracySessionSummaryResponse

**File:** `AccuracySessionSummaryResponse.java`

**Description:**
A lightweight summary DTO for listing or briefly showing accuracy sessions. Typically used in session list views.

**Fields:**

* Session ID
* User ID or nickname
* Song title
* Mode
* Average score
* Created date

---

## ChallengeSessionDto

**File:** `ChallengeSessionDto.java`

**Description:**
Represents data used when creating or transferring challenge session info. May be used in both request and response flows.

**Fields Might Include:**

* Challenge ID
* Start time / end time
* Song ID
* Mode or status

---

## ChallengeSessionResponse

**File:** `ChallengeSessionResponse.java`

**Description:**
DTO returned after a challenge session is completed or queried. Contains result details and metadata about the session.

**Common Fields:**

* Challenge session ID
* Song info (title, artist)
* Start and end times (seconds)
* Timestamps

---

## ChatCompletionRequest

**File:** `ChatCompletionRequest.java`

**Description:**
Request body format used to interact with GPT-based backend service. Contains chat prompt structure for GPT inference.

**Likely Fields:**

* List of `ChatMessage` objects
* Model name (e.g., gpt-3.5-turbo)
* Temperature, maxTokens, etc.

---

## ChatCompletionResponse

**File:** `ChatCompletionResponse.java`

**Description:**
Response wrapper for GPT-based completion, typically containing the generated message.

**Includes:**

* List of message choices
* Usage statistics (e.g., tokens used)

---

## ChatMessage

**File:** `ChatMessage.java`

**Description:**
Encapsulates a single message in a chat-style prompt. Compatible with OpenAI's chat API structure.

**Fields:**

* `role`: e.g., "user", "assistant"
* `content`: Message content text

---

## CorrectionResponse

**File:** `CorrectionResponse.java`

**Description:**
Represents a feedback or correction message generated for a specific frame or session. Typically used to send back GPT-based feedback.

**Fields Might Include:**

* Frame index or session ID
* Feedback text
* Feedback type or tag

---

## CustomUserDetails

**File:** `CustomUserDetails.java`

**Description:**
Implements Spring Security's `UserDetails` to wrap around the `AppUser` entity. Used for integrating user authentication with Spring Security.

**Responsibilities:**

* Provide user credentials (username, password)
* Expose authorities/roles
* Mark account as active, locked, expired, etc.

---

## LoginRequest

**File:** `LoginRequest.java`

**Description:**
Represents the payload for a user login request. Typically sent from the frontend login form.

**Fields:**

* `username`: User's login identifier
* `password`: Corresponding password

**Used In:**

* `LoginFilter`
* AuthenticationController or SecurityConfig

---

## JoinRequest

**File:** `JoinRequest.java`

**Description:**
Data transfer object for user registration. Collects all necessary signup fields.

**Fields Might Include:**

* `username`, `email`, `password`
* Optional: `profileImageUrl`, `role`

**Used In:**

* UserController or AuthController
* Registration logic and user creation

---

## EditNameRequest

**File:** `EditNameRequest.java`

**Description:**
Payload used when a user updates their display name.

**Fields:**

* `newName`: The updated username or nickname

**Used In:**

* ProfileController or UserController

---

## EditPasswordRequest

**File:** `EditPasswordRequest.java`

**Description:**
DTO for updating the user's password.

**Fields:**

* `currentPassword`: Current password for verification
* `newPassword`: New password to be applied

**Used In:**

* Account settings or security update flows

---

## EditProfileImageRequest

**File:** `EditProfileImageRequest.java`

**Description:**
Used when a user updates their profile picture.

**Fields:**

* `imageUrl`: New URL or image path to be saved as the profile picture

---

## fakeFeedbackResponse

**File:** `fakeFeedbackResponse.java`

**Description:**
Likely a mock or placeholder DTO for testing GPT feedback or simulated evaluation output.

**Fields Might Include:**

* `frameIndex`: Targeted frame number
* `feedback`: Textual response, possibly generated or dummy

**Note:** Not used in production logic; retained for testing or demonstration purposes.

---

## LowScoreFeedbackResponse

**File:** `LowScoreFeedbackResponse.java`

**Description:**
A response DTO containing GPT-based feedback for frames or segments that received low accuracy scores. Used to assist users in improving performance.

**Fields Might Include:**

* `frameIndex`: The frame or section identifier
* `feedback`: Textual coaching or correction
* `tag`: Optional label for the type of error

---

## MyVideoResponse

**File:** `MyVideoResponse.java`

**Description:**
DTO used to return a list of recorded videos belonging to the current user. Used in personal dashboard or video history views.

**Fields Likely Include:**

* `videoId`
* `videoPath`
* `mode` (practice, challenge, accuracy)
* `recordedAt`, `duration`
* Possibly session-related metadata (song title, session type)

---

## PasswordResetRequest

**File:** `PasswordResetRequest.java`

**Description:**
Request DTO used to initiate a password reset process, often via email verification.

**Fields:**

* `email`: Registered email to receive the reset code or link

**Used In:**

* Password recovery flows

---

## PasswordResetVerifyRequest

**File:** `PasswordResetVerifyRequest.java`

**Description:**
DTO for verifying the reset code that was sent to the user’s email.

**Fields:**

* `email`: Email address used
* `code`: Verification code received by the user

---

## PasswordResetNewPasswordRequest

**File:** `PasswordResetNewPasswordRequest.java`

**Description:**
Final step in password reset flow. Allows user to set a new password after successful verification.

**Fields:**

* `email`: Email associated with the account
* `newPassword`: New password to be saved

---

## UserResponseDto

**File:** `UserResponseDto.java`

**Description:**
DTO used to deliver user account data (e.g., to a profile page or admin panel). Wraps selected fields from the `AppUser` entity.

**Fields May Include:**

* `id`, `username`, `email`, `role`
* `profileImageUrl`
* Possibly timestamps (createdAt, updatedAt)

---

## UserProfile

**File:** `UserProfile.java`

**Description:**
A user-specific DTO typically returned for personal profile views. Provides user-specific summary info without exposing sensitive data.

**Fields Likely Include:**

* `username`, `email`
* `profileImageUrl`
* Session stats (e.g., number of sessions, best score)

---

## PracticeSessionDto

**File:** `PracticeSessionDto.java`

**Description:**
DTO for creating or submitting a new practice session. Sent from the client when starting or ending a session.

**Fields Might Include:**

* `songId`
* `startTime`, `endTime`
* `mode`

---

## PracticeSessionResponse

**File:** `PracticeSessionResponse.java`

**Description:**
DTO used to return information about a completed or ongoing practice session.

**Fields Include:**

* `sessionId`
* `songTitle`
* `startTime`, `endTime`, `mode`, `createdAt`

---

## RecordedVideoDto

**File:** `RecordedVideoDto.java`

**Description:**
DTO used for creating or representing video records. Useful for mapping video metadata without exposing full entity structure.

**Fields May Include:**

* `videoId`, `videoPath`, `duration`
* `mode`, `recordedAt`, `sessionId`

---

## SongDto

**File:** `SongDto.java`

**Description:**
Compact representation of a `Song` entity. Returned when listing songs or searching.

**Fields:**

* `id`, `title`, `artist`, `genre`
* Possibly cover path, audio path

---

## SongSearchDto

**File:** `SongSearchDto.java`

**Description:**
DTO used to capture user input for searching songs in the catalog.

**Fields Might Include:**

* `title`, `artist`, `genre` (optional search filters)

**Used In:**

* Search endpoints for songs
* Dynamic filtering in frontend UI
