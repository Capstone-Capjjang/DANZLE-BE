# OAuth2 Class Descriptions

This section documents the class located in the `oauth2` package, handling OAuth2 login success scenarios.

---

## OAuth2AuthenticationSuccessHandler

**File:** `OAuth2AuthenticationSuccessHandler.java`

**Description:**
This class handles successful OAuth2 login attempts. When a user logs in via a third-party provider (e.g., Google), this handler is invoked.

**Responsibilities:**

* Processes the OAuth2 `Authentication` object
* Extracts user information (such as email)
* Checks for user existence in the database
* Registers the user if new, or updates login metadata if existing
* Issues a JWT token for the authenticated OAuth2 user
* Sends the token to the frontend (e.g., via `HttpServletResponse`)

**Typical Use Case:**
Used in conjunction with Spring Security's OAuth2 client support to seamlessly integrate third-party login with JWT-based session handling.
