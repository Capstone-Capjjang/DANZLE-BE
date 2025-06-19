# Entity Class Descriptions

This document provides a concise overview of all entity classes located in the `entity` package. Each entity is part of a JPA-based backend system used for video analysis and user session tracking.

---

## AccuracyFrameEvaluation

**File:** `AccuracyFrameEvaluation.java`

**Description:**
Represents the evaluation result of a specific frame during an accuracy analysis session. Stores frame-level scores and optional GPT-generated feedback.

**Fields:**

* `id`: Unique identifier (`Long`)
* `session`: Reference to the associated `AccuracySession` (`ManyToOne`)
* `frameIndex`: Frame index within the session (`Integer`)
* `score`: Accuracy score for the frame (`double`)
* `resultTag`: Qualitative result tag (e.g., "good", "bad\_arm") (`String`)
* `gptFeedback`: GPT-based feedback for the frame (`String`)

---

## AccuracySession

**File:** `AccuracySession.java`

**Description:**
Represents a full session of accuracy-based evaluation. It stores metadata about the session such as time, mode, and average score.

**Fields:**

* `id`: Session ID (`Long`)
* `user`: User who performed the session (`AppUser`, `ManyToOne`)
* `song`: Evaluated song (`Song`, `ManyToOne`)
* `startTime`, `endTime`: Start and end timestamps (`LocalDateTime`)
* `avgScore`: Average score across all frames (`Double`)
* `mode`: Evaluation mode (e.g., "full", "highlight") (`String`)
* `createdAt`: Timestamp of session creation (`LocalDateTime`)

---

## AppUser

**File:** `AppUser.java`

**Description:**
Represents a user account within the system. Used for authentication, session tracking, and role-based access control.

**Fields:**

* `id`: User ID (`Long`)
* `username`, `password`, `email`: Basic authentication info (`String`)
* `role`: User role (`Role`, Enum)
* `profileImageUrl`: Profile image path (`String`)

---

## ChallengeSession

**File:** `ChallengeSession.java`

**Description:**
Represents a session in challenge mode, where users attempt specific segments of a song.

**Fields:**

* `id`: Unique session ID (`Long`)
* `user`: Associated user (`AppUser`)
* `song`: Challenge song (`Song`)
* `sessionId`: Custom session ID used on the frontend (`Long`)
* `startTime`, `endTime`: Time interval of the challenge in seconds (`int`)

---

## PracticeSession

**File:** `PracticeSession.java`

**Description:**
Represents a practice session where users can freely repeat movements without accuracy evaluation.

**Fields:**

* `id`: Unique session ID (`Long`)
* `user`: Performing user (`AppUser`)
* `song`: Practiced song (`Song`)
* `startTime`, `endTime`: Practice session timestamps (`LocalDateTime`)
* `mode`: Practice mode (`String`)
* `createdAt`: Session creation time (`LocalDateTime`)

---

## RecordedVideo

**File:** `RecordedVideo.java`

**Description:**
Stores metadata of recorded videos linked to sessions such as practice, challenge, or accuracy evaluation.

**Fields:**

* `id`: Unique video ID (`Long`)
* `user`: Owner of the video (`AppUser`)
* `mode`: Type of session (`VideoMode`, Enum)
* `practiceSession`: Related practice session (`PracticeSession`)
* `challengeSession`: Related challenge session (`ChallengeSession`)
* `accuracySession`: Related accuracy session (`AccuracySession`)
* `videoPath`: File path of the video (`String`)
* `recordedAt`: Recording timestamp (`LocalDateTime`)
* `duration`: Video length in seconds (`int`)

---

## Song

**File:** `Song.java`

**Description:**
Represents a song entity containing metadata and resource paths used for user sessions.

**Fields:**

* `id`: Song ID (`Long`)
* `title`, `artist`, `genre`: Song metadata (`String`)
* `fullStartTime`, `fullEndTime`: Full duration boundaries (`Integer`)
* `highlightStartTime`, `highlightEndTime`: Highlight segment boundaries (`Integer`)
* `audioFilePath`: Path to the audio file (`String`)
* `silhouetteVideoPath`: Path to silhouette guide video (`String`)
* `danceGuidePath`: Path to dance tutorial (`String`)
* `avatarVideoWithAudioPath`: Avatar video with audio (`String`)
* `coverImagePath`: Album art or cover image (`String`)
* `createdBy`: Creator or uploader of the song (`String`)

---

## Role

**File:** `Role.java`

**Description:**
An enumeration used for defining user roles in the system. Supports Spring Security role prefixing.

**Enum Values:**

* `USER`: Standard user
* `ADMIN`: Administrator

**Methods:**

* `toString()`: Returns the role name prefixed with `"ROLE_"` (e.g., `ROLE_USER`)

---

## VideoMode

**File:** `VideoMode.java`

**Description:**
An enumeration representing the type of session a video belongs to.

**Enum Values:**

* `PRACTICE("Practice mode")`: General practice session
* `CHALLENGE("Challenge mode")`: Time-constrained challenge session
* `ACCURACY("Accuracy mode")`: Accuracy evaluation session

**Fields & Methods:**

* `description`: A string description of the mode
* `getDescription()`: Returns the human-readable description
