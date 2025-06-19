# 1. AuthController API Documentation

This controller manages user authentication processes including registration, social login redirection, and JWT token refreshing.

---

### 1. POST `/join`

**Purpose**  
Registers a new user.

**Method**  
POST

**Request Body**
```json
{
  "name": "username",
  "email": "user@example.com",
  "password1": "password123",
  "password2": "password123"
}
```

**Behavior**

- Validates input using `@Valid` and `BindingResult`.
- Checks if `password1` and `password2` match.
- If valid and not duplicated, creates a new user.
- Handles:
  - Field validation errors (returns field: error message pairs).
  - Password mismatch (`400 Bad Request`).
  - Duplicate email (`400 Bad Request` with message `"User already exists"`).
  - Unexpected errors (`400 Bad Request` with specific message).

**Responses**
- `200 OK`: `"User successfully registered."`
- `400 Bad Request`: with error message(s)

---

### 2. GET `/loginNaver`, `/loginGoogle`

**Purpose**  
Redirects to the social login interface (Naver or Google).

**Method**  
GET

**Response**
- Returns `ModelAndView` pointing to `"socialLogin"` view.

---

### 3. POST `/refresh`

**Purpose**  
Generates a new access token from a valid refresh token.

**Method**  
POST

**Headers**
```
Refresh-Token: <your-refresh-token>
```

**Behavior**

- Checks whether the refresh token is expired.
- If valid, extracts email from token and creates a new access token (valid for 1 hour).

**Responses**

- `200 OK`  
```json
{
  "accessToken": "<new-access-token>"
}
```

- `401 Unauthorized`  
```json
{
  "error": "Refresh token expired"
}
```

---

## Summary Table

| Method | Endpoint       | Description                        |
|--------|----------------|------------------------------------|
| POST   | `/join`        | Registers a new user               |
| GET    | `/loginNaver`  | Redirects to Naver login view      |
| GET    | `/loginGoogle` | Redirects to Google login view     |
| POST   | `/refresh`     | Issues new access token by refresh |

---

# 2. AccuracySessionController API Documentation

This controller handles operations related to accuracy sessions, including session creation, frame-by-frame analysis, result saving, and summary generation.

---

### 1. GET `/accuracy-session/user/me`

**Purpose**  
Fetches all accuracy sessions associated with the currently authenticated user.

**Method**  
GET

**Response**
- 200 OK: List of AccuracySessionResponse objects
- 400 Bad Request: Invalid input
- 500 Internal Server Error: Unexpected server error

---

### 2. GET `/accuracy-session/song/{songId}/user/me`

**Purpose**  
Fetches accuracy sessions for the current user and a specific song.

**Method**  
GET

**Path Variables**
- songId(Long): Song identifier

**Authentication**
Required

**Response**
- 200 OK: List of sessions
- 400 Bad Request: Invalid song or user
- 500 Internal Server Error: Unexpected failure

---

### 3. POST `/accuracy-session/analyze`

**Purpose**  
Analyzes a single user frame (image) by sending it to the Flask server, stores the frame result, and links it to the corresponding session.

**Method**  
POST

**Content-Type**  
multipart/form-data

**Authentication**
Required

**Request Parameters**
- sec: Integer (time in seconds)
- songId: Long
- sessionId: Long
- frame: MultipartFile (image)

**Response**
- 200 OK: Analysis result including score, direction, etc.
- 400 Bad Request: Invalid parameters or session
- 500 Internal Server Error: Communication or processing error

---

### 4. POST `/accuracy-session/save`

**Purpose**  
Saves the final results of an accuracy session after practice is complete.

**Method**  
POST

**Authentication**
Required

**Request Parameters**
- sessionId: Long

**Response**
- 200 OK: Session summary and feedback
- 400 Bad Request: Session invalid or already finalized
- 500 Internal Server Error: GPT or save failure

---

### 5. GET `/accuracy-session/result`

**Purpose**  
Retrieves detailed result for a specific accuracy session.

**Method**  
GET

**Query Parameters**
- sessionId: Long

**Response**
- 200 OK: Full session result
- 404 Not Found: Session not found
- 500 Internal Server Error: Unexpected error

---

### 6. GET `/accuracy-session/video-paths`

**Purpose**  
Returns silhouette video paths for a song by its name.

**Method**  
GET

**Query Parameters**
- songName: String

**Response**
- 200 OK: List of video path strings
- 400 Bad Request: Song not found or invalid input
- 500 Internal Server Error: Unexpected failure

---

### 7. POST `/accuracy-session/full`

**Purpose**  
Starts a full-section (e.g., verse 1) accuracy session for the user and specified song.

**Method**  
POST

**Authentication**
Required

**Query Parameters**
- songId: Long

**Response**
- 200 OK: CorrectionResponse (session ID, song title)
- 400 Bad Request: Invalid song ID or user
- 500 Internal Server Error: Session creation failure

---

### 8. GET `/accuracy-session/summary`

**Purpose**  
Provides summarized accuracy statistics after all frames in a session are analyzed.

**Method**  
GET

**Query Parameters**
- sessionId: Long

**Response**
- 200 OK: Summary statistics and final results
- 400 Bad Request: Missing or invalid session ID
- 404 Not Found: Session not found
- 500 Internal Server Error: Summary generation failure

---

## Summary Table

| Method | Endpoint                                  | Description                                        |
|--------|-------------------------------------------|----------------------------------------------------|
| GET    | /accuracy-session/user/me                 | Get all sessions for current user                 |
| GET    | /accuracy-session/song/{songId}/user/me   | Get sessions for song + user                      |
| POST   | /accuracy-session/analyze                 | Analyze user frame via Flask and store result     |
| POST   | /accuracy-session/save                    | Save full session result and trigger GPT feedback |
| GET    | /accuracy-session/result                  | Retrieve detailed session results                 |
| GET    | /accuracy-session/video-paths             | Get silhouette video paths by song name           |
| POST   | /accuracy-session/full                    | Start a full-mode accuracy session                |
| GET    | /accuracy-session/summary                 | Get summary statistics for an accuracy session    |

---

# 3. PracticeSessionController API Documentation

This controller manages practice session operations, including session creation, retrieval, video path access, and result saving.

---

### 1. GET `/practice-session/user/me`

**Purpose**  
Fetches all practice sessions of the currently authenticated user.

**Method**  
GET

**Response**
- 200 OK: List of `PracticeSessionResponse` DTOs
- 401 Unauthorized: If user is not authenticated

---

### 2. GET `/practice-session/song/{songId}/user/me`

**Purpose**  
Fetches practice sessions of the authenticated user for a specific song.

**Method**  
GET

**Path Variables**
- `songId`: ID of the song

**Response**
- 200 OK: List of `PracticeSessionResponse` DTOs
- 401 Unauthorized: If user is not authenticated

---

### 3. POST `/practice-session/full`

**Purpose**  
Starts a full-section (first verse) practice session for the authenticated user and the given song.

**Method**  
POST

**Query Parameters**
- `songId`: ID of the song

**Response**
- 200 OK: A list with one `PracticeSessionResponse` object
- 400 Bad Request: Invalid input (e.g. song not found or already in session)
- 500 Internal Server Error: On failure to create session

---

### 4. POST `/practice-session/highlight`

**Purpose**  
Starts a highlight-section practice session for the authenticated user and the given song.

**Method**  
POST

**Query Parameters**
- `songId`: ID of the song

**Response**
- 200 OK: A list with one `PracticeSessionResponse` object
- 400 Bad Request: Invalid input
- 500 Internal Server Error: On failure to create session

---

### 5. POST `/practice-session/save`

**Purpose**  
Saves the final result of the practice session after the user finishes practicing.

**Method**  
POST

**Query Parameters**
- `sessionId` (Long): ID of the practice session

**Response**
- 200 OK: Summary or result object
- 400 Bad Request: Invalid session
- 500 Internal Server Error: Save failure

---

### 6. GET `/practice-session/video-paths`

**Purpose**  
Returns silhouette video paths for a song based on its title.

**Method**  
GET

**Query Parameters**
- `songName`: Title of the song (String)

**Response**
- 200 OK: List of video paths (e.g., URLs or file paths)
- 400 Bad Request: If song not found or name invalid
- 500 Internal Server Error: Server error while retrieving data

---

## Summary Table

| Method | Endpoint                                | Description                                             |
| ------ | --------------------------------------- | ------------------------------------------------------- |
| GET    | /practice-session/user/me               | Get all practice sessions of the current user           |
| GET    | /practice-session/song/{songId}/user/me | Get practice sessions for a specific song and user      |
| POST   | /practice-session/full                  | Start a full-section (verse 1) practice session         |
| POST   | /practice-session/highlight             | Start a highlight-section practice session              |
| POST   | /practice-session/save                  | Save the result of a completed practice session         |
| GET    | /practice-session/video-paths           | Get silhouette video paths by song title (shared logic) |


---

# 4. ChallengeSessionController API Documentation

This controller manages user challenge sessions including session creation, result retrieval, background/highlight resources, and saving results.

---

### 1. GET `/challenge-session/user/me`

**Purpose**  
Retrieve all challenge sessions of the currently authenticated user.

**Method**  
GET

**Response**
- 200 OK: List of `ChallengeSessionResponse` DTOs
- 401 Unauthorized: If user is not authenticated

---

### 2. GET `/challenge-session/song/{songId}/user/me`

**Purpose**  
Retrieves challenge sessions for a specific song and the authenticated user.

**Method**  
GET

**Path Variables**
- `songId`: ID of the song

**Response**
- 200 OK: List of `ChallengeSessionResponse` DTOs
- 401 Unauthorized: If user is not authenticated

---

### 3. GET `/challenge-session/result`

**Purpose**  
Retrieves detailed information for a specific challenge session ID.

**Method**  
GET

**Query Parameters**
- `sessionId`: ID of the session (as query param)

**Response**
- 200 OK: `ChallengeSessionResponse` DTO
- 404 Not Found: If session does not exist
- 500 Internal Server Error: On unexpected failure

---

### 4. GET `/challenge-session/background/{songId}`

**Purpose**  
Returns the background resource (video path) used in the challenge mode for the given song.

**Method**  
GET

**Path Variables**
- `songId`: ID of the song

**Response**
- 200 OK: Background path as a plain string

---

### 5. GET `/challenge-session/highlight/{songId}`

**Purpose**  
Returns the highlight section data of a song used for challenge mode.
Also creates and saves the challenge session for the current user.

**Method**  
GET

**Path Variables**
- `songId`: ID of the song

**Authentication**
- Required (CustomUserDetails)

**Response**
- 200 OK: Highlight section string (e.g., timestamp or ID)
- 401 Unauthorized: If user is not authenticated

---

### 6. GET `/challenge-session/highlight`

**Purpose**  
Start a new challenge session in highlight mode for the authenticated user.

**Method**  
POST

**Path Variables**
- `songId`: ID of the song

**Response**
- 200 OK: A list containing one ChallengeSessionResponse
- 400 Bad Request: Invalid input
- 500 Internal Server Error: Session creation failure

---

### 7. GET `/challenge-session/save`

**Purpose**  
Save the final result of a challenge session.

**Method**  
POST

**Path Variables**
- `sessionId` (Long): ID of the challenge session

**Response**
- 200 OK: Challenge result summary
- 400 Bad Request: If session is invalid
- 500 Internal Server Error: Save failure

---

## Summary Table

| Method | Endpoint                                 | Description                                                 |
| ------ | ---------------------------------------- | ----------------------------------------------------------- |
| GET    | /challenge-session/user/me               | Get all challenge sessions for current user                 |
| GET    | /challenge-session/song/{songId}/user/me | Get challenge sessions for specific song and current user   |
| GET    | /challenge-session/result                | Get challenge session result by session ID                  |
| GET    | /challenge-session/background/{songId}   | Get background resource used in challenge mode              |
| GET    | /challenge-session/highlight/{songId}    | Get highlight section for challenge mode and create session |
| POST   | /challenge-session/highlight             | Start a highlight-mode challenge session                    |
| POST   | /challenge-session/save                  | Save final result of a challenge session                    |


---

# 5. SongController API Documentation

This controller handles song management and retrieval, including searching, guide video info, and song registration.

---

### 1. GET `/song/all`

**Purpose**  
Retrieve all songs or filter songs by a keyword in the title or artist.

**Method**  
GET

**Query Parameters**
- `keyword` (optional): If provided, filters songs by keyword in title or artist.

**Response**
- 200 OK:
```json
[
  {
    "id": 1,
    "title": "Song Title",
    "artist": "Artist Name",
    "coverImagePath": "path/to/image.jpg"
  }
]
```

- 500 Internal Server Error:
```json
{
  "error": "Internal Server Error"
}
```

---

### 2. GET `/song/search`

**Purpose**  
Search songs by title and/or artist.

**Method**  
GET

**Query Parameters**
- `title` (optional): Song title to search
- `artist` (optional): Artist name to search

**Response**
- 200 OK:
```json
[
  {
    "id": 2,
    "title": "Another Song",
    "artist": "Another Artist",
    "coverImagePath": "another/path.jpg"
  }
]
```

- 404 Not Found:
```json
{
  "error": "No search results found"
}
```

- 500 Internal Server Error:
```json
{
  "error": "Internal Server Error"
}
```

---

### 3. GET `/song/{songId}/song-info`

**Purpose**  
Returns dance guide video and section timing info for a given song.

**Method**  
GET

**Path Variables**
- `songId` (required): ID of the song

**Query Parameters**
- `mode` (optional): `'full'` or `'highlight'` – only for practice mode
- `for` (optional): `'accuracy'` or `'practice'` – determines info format

**Response**
- 200 OK (accuracy):
```json
{
  "silhouetteVideoUrl": "url/to/silhouette.mp4"
}
```

- 200 OK (practice):
```json
{
  "silhouetteVideoUrl": "url/to/silhouette.mp4",
  "startTime": 15.0,
  "endTime": 45.0
}
```

- 200 OK (invalid mode):
```json
{
  "silhouetteVideoUrl": "url/to/silhouette.mp4",
  "message": "Invalid mode. Use 'full' or 'highlight'."
}
```

---

### 4. POST `/song/add-song`

**Purpose**  
Registers a new song 

**Method**  
POST

**Authentication**
- Requires user with `ROLE_ADMIN`

**Request Body**
```json
{
  "title": "New Song",
  "artist": "New Artist",
  "coverImagePath": "cover/path.jpg"
}
```

**Response**
- 200 OK:
```json
{
  "id": 3,
  "title": "New Song",
  "artist": "New Artist",
  "coverImagePath": "cover/path.jpg"
}
```

- 500 Internal Server Error:
```json
{
  "error": "Internal Server Error"
}
```
## Summary Table

| Method | Endpoint                 | Description                                     |
| ------ | ------------------------ | ----------------------------------------------- |
| GET    | /song/all                | Get all songs or filter by keyword              |
| GET    | /song/search             | Search songs by title and/or artist             |
| GET    | /song/{songId}/song-info | Get silhouette video and timing info for a song |
| POST   | /song/add-song           | Register a new song (Admin only)                |


---

# 6. VideoController API Documentation

This controller manages operations related to recorded videos, including listing, retrieving by session, saving new uploads, and replacing existing files.

---

### 1. GET `/recorded-video/user/me`

**Purpose**  
Retrieve all recorded videos of the authenticated user, optionally filtered by video mode.

**Method**  
GET

**Query Parameters**  
- `mode` (optional): Enum value of `VideoMode` (`PRACTICE`, `CHALLENGE`, `ACCURACY`)

**Authentication**  
Required (`@AuthenticationPrincipal`)

**Response**  
- 200 OK  
```json
[
  {
    "videoId": 1,
    "videoMode": "PRACTICE",
    "duration": 30,
    "recordedAt": "2025-06-17T12:00:00",
    "videoPath": "/videos/1.mp4"
  }
]
```
- 401 Unauthorized

---

### 2. GET `/recorded-video/{videoId}`

**Purpose**  
Retrieve metadata for a specific recorded video.

**Method**  
GET

**Path Variables**  
- `videoId`: Long, ID of the recorded video

**Response**  
- 200 OK  
```json
{
  "videoId": 2,
  "videoMode": "CHALLENGE",
  "duration": 45,
  "recordedAt": "2025-06-16T15:00:00",
  "videoPath": "/videos/2.mp4"
}
```
- 404 Not Found  
```json
{
  "error": "Video not found with ID: {videoId}"
}
```

---

### 3. GET `/recorded-video/session`

**Purpose**  
Retrieve recorded videos by session ID and video mode.

**Method**  
GET

**Query Parameters**  
- `sessionId`: Long, ID of the session  
- `mode`: Enum value of `VideoMode`

**Response**  
- 200 OK  
```json
[
  {
    "videoId": 3,
    "videoMode": "ACCURACY",
    "duration": 40,
    "recordedAt": "2025-06-15T10:30:00",
    "videoPath": "/videos/3.mp4"
  }
]
```

---

### 4. POST `/recorded-video/saveVideo`

**Purpose**  
Save a newly recorded video along with its metadata.

**Method**  
POST (multipart/form-data)

**Authentication**  
Required

**Request Parameters**  
- `file`: MultipartFile, video file  
- `sessionId`: Long  
- `videoMode`: Enum (`PRACTICE`, `CHALLENGE`, `ACCURACY`)  
- `recordedAt`: ISO-8601 datetime string (e.g., `2025-06-17T12:00:00`)  
- `duration`: int (seconds)

**Response**  
- 200 OK  
```json
{
  "videoId": 4,
  "videoMode": "PRACTICE",
  "duration": 60,
  "recordedAt": "2025-06-17T12:00:00",
  "videoPath": "/videos/4.mp4"
}
```
- 400 Bad Request  
- 401 Unauthorized

---

### 5. POST `/recorded-video/{videoId}/edit`

**Purpose**  
Replace an existing recorded video with a new file.

**Method**  
POST (multipart/form-data)

**Path Variables**  
- `videoId`: Long, ID of the video to replace

**Request Parameters**  
- `file`: MultipartFile, new video file

**Response**  
- 200 OK  
```json
{
  "message": "Video updated successfully."
}
```
- 400 Bad Request  
```json
{
  "error": "Uploaded file is empty."
}
```
## Summary Table

| Method | Endpoint                       | Description                                        |
| ------ | ------------------------------ | -------------------------------------------------- |
| GET    | /recorded-video/user/me        | Get all videos of current user, optionally by mode |
| GET    | /recorded-video/{videoId}      | Get details of a specific recorded video           |
| GET    | /recorded-video/session        | Get videos by session ID and mode                  |
| POST   | /recorded-video/saveVideo      | Save a new recorded video                          |
| POST   | /recorded-video/{videoId}/edit | Replace an existing video file                     |


---

# 7. UserController API Documentation

This controller handles user profile operations, including viewing user information, editing name/password/image, and deleting the account.

---

### 1. GET `/user/profile`

**Purpose**  
Retrieve full profile information for the authenticated user, including personal data, uploaded videos, and accuracy session records.

**Method**  
GET

**Authentication**  
Required

**Response**  
- 200 OK: Returns a `UserResponseDto` containing user profile, videos, and accuracy records.

---

### 2. PATCH `/user/profile/edit/name`

**Purpose**  
Update the authenticated user’s display name.

**Method**  
PATCH

**Authentication**  
Required

**Request Body**
```json
{
  "name": "newName"
}
```

**Validation**  
- Name must meet validation criteria defined in `EditNameRequest`

**Response**  
- 200 OK: 이름이 성공적으로 수정되었습니다.
- 400 Bad Request: If validation fails

---

### 3. PATCH `/user/profile/edit/password`

**Purpose**  
Change the authenticated user's password.

**Method**  
PATCH

**Authentication**  
Required

**Request Body**
```json
{
  "currentPassword": "currentPassword123",
  "newPassword1": "newPassword456",
  "newPassword2": "newPassword456"
}
```

**Behavior**  
- Validates that current password is correct
- Ensures new passwords match and meet criteria

**Response**  
- 200 OK: 비밀번호가 성공적으로 수정되었습니다.
- 400 Bad Request: 현재 비밀번호가 일치하지 않습니다 or validation errors

---

### 4. PATCH `/user/profile/edit/img`

**Purpose**  
Update the authenticated user's profile image.

**Method**  
PATCH

**Authentication**  
Required

**Consumes**  
`multipart/form-data`

**Request Parameters**  
- `file`: Multipart image file

**Response**  
- 200 OK: 프로필 이미지가 성공적으로 수정되었습니다.
- 400 Bad Request: If file is empty

---

### 5. DELETE `/user/profile/deleteAccount`

**Purpose**  
Delete the authenticated user's account.

**Method**  
DELETE

**Authentication**  
Required

**Response**  
- 200 OK: 계정이 성공적으로 삭제되었습니다.

## Summary Table

| Method | Endpoint                    | Description                |
| ------ | --------------------------- | -------------------------- |
| GET    | /user/profile               | Retrieve full user profile |
| PATCH  | /user/profile/edit/name     | Edit user name             |
| PATCH  | /user/profile/edit/password | Change user password       |
| PATCH  | /user/profile/edit/img      | Update user profile image  |
| DELETE | /user/profile/deleteAccount | Delete user account        |


---

# 8. PasswordResetController API Documentation

This controller manages the password reset process, including email verification and setting a new password.

---

### 1. POST `/password-reset/request`

**Purpose**  
Send a verification code to the user's email for password reset.

**Method**  
POST

**Request Body**
- email (string): The user's email address

**Validation**
- Email format must be valid

**Response**
- 200 OK: Verification code sent.
- 400 Bad Request: If email is invalid or format check fails

---

### 2. POST `/password-reset/verify`

**Purpose**  
Verify the code sent to the user's email.

**Method**  
POST

**Request Body**
- email (string): The user's email address  
- verificationCode (string): The code sent to the user's email

**Validation**
- Email and verification code must meet format constraints

**Response**
- 200 OK: Verification successful.
- 400 Bad Request: Invalid code or validation failure

---

### 3. POST `/password-reset/new-password`

**Purpose**  
Set a new password after successful code verification.

**Method**  
POST

**Request Body**
- email (string): The user's email address  
- newPassword1 (string): New password  
- newPassword2 (string): Confirmation of new password

**Validation**
- Passwords must match  
- Password format must meet validation rules

**Response**
- 200 OK: Password successfully reset.
- 400 Bad Request:
  - If passwords do not match  
  - If format validation fails

## Summary Table

| Method | Endpoint                     | Description                            |
| ------ | ---------------------------- | -------------------------------------- |
| POST   | /password-reset/request      | Send verification code to user's email |
| POST   | /password-reset/verify       | Verify email and code                  |
| POST   | /password-reset/new-password | Set new password after verification    |


---

# 9. S3Controller API Documentation (not used)

This controller is defined but currently **not used** in the application logic.

---

### 1. GET `/api/s3/presigned-url`

**Purpose**  
Generates a presigned URL that allows clients to upload a video file directly to S3 without needing AWS credentials.

**Method**  
GET

**Query Parameters**
- `filename` (string, required): The name of the file to be uploaded (e.g., `video123.mp4`). The file will be saved under the `videos/` directory in the S3 bucket.

**Response**
- 200 OK: Returns a presigned URL as a plain string
- 500 Internal Server Error: If URL generation fails

**Notes**
- The presigned URL is valid for 10 minutes
- The content type is fixed to `video/mp4`
- The file will be stored in the following S3 path format: `videos/{filename}`

---

# 10. AiFeedbackController API Documentation

This controller provides GPT-based AI feedback features using image-based or session-based evaluation results.

---

### 1. GET `/api/image-feedback`

**Purpose**

Provides GPT feedback using two image files (user and expert) as input. Mainly used for testing purposes.

**Method**

GET

**Request Parameters**

- `userImagePath` (String): Path to the user image file (will be base64-encoded)

- `expertImagePath` (String): Path to the expert image file (will be base64-encoded)

**Behavior**

- Converts both images to base64

- Sends the images to OpenAI GPT API with a fixed prompt

- Returns GPT-generated feedback about pose differences

**Responses**

- `200 OK`

Returns a `String` with GPT-generated feedback.

- `400 Bad Request`

If file paths are invalid or feedback fails.

---

### 2. GET `/api/low-score-feedback`

**Purpose**

Generates GPT feedback based on low-score sections of an accuracy session.

**Method**

GET

**Request Parameters**

- `sessionId` (Long): ID of the accuracy session

**Behavior**

- Loads stored analysis results linked to the session
- Extracts the lowest-scoring frames
- Calls OpenAI GPT with preformatted prompts using pose JSON
- Returns GPT feedback covering multiple low-score segments

**Responses**

- `200 OK`
- `400 Bad Request`
    
    If session is invalid or analysis data is missing
    
- `500 Internal Server Error`
    
    If GPT communication fails
    

## Summary Table

| Method | Endpoint | Description |
| --- | --- | --- |
| GET | /api/image-feedback | Generate GPT feedback from user vs expert image |
| GET | /api/low-score-feedback | Generate GPT feedback based on session analysis |
