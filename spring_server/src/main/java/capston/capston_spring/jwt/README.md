# JWT Class Descriptions

This section describes classes located in the `jwt` package, responsible for handling JWT-based authentication and authorization.

---

## JWTFilter

**File:** `JWTFilter.java`

**Description:**
A `OncePerRequestFilter` implementation that intercepts HTTP requests and validates JWT tokens included in the `Authorization` header. If the token is valid, it sets the authentication in the security context.

**Responsibilities:**

* Extracts the JWT from the request header
* Validates the token using `JWTUtil`
* Sets the authenticated user in the Spring Security context

---

## JWTUtil

**File:** `JWTUtil.java`

**Description:**
Utility class for creating, parsing, and validating JWT tokens. It encapsulates logic for:

* Generating access tokens with claims
* Validating token signatures and expiration
* Extracting user-related information (e.g., username, roles) from token payload

**Common Methods:**

* `generateToken()`: Creates JWT based on user credentials
* `validateToken()`: Checks token signature and expiration
* `getUsername()`: Extracts subject (username) from token

---

## LoginFilter

**File:** `LoginFilter.java`

**Description:**
A filter that processes login requests. It extends `UsernamePasswordAuthenticationFilter` and is responsible for:

* Parsing username and password from the request
* Authenticating credentials with `AuthenticationManager`
* Generating a JWT token upon successful authentication
* Returning the token in the HTTP response

**Key Steps:**

1. Override `attemptAuthentication()` to extract credentials
2. On success, override `successfulAuthentication()` to issue JWT
