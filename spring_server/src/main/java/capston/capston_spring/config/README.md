# config/ Directory Overview

The `capston_spring/config/` directory contains essential configuration files for the Spring Boot application. These classes define how the application handles security, Redis, AWS S3, Swagger, Web requests, and external API communication.

---

## Configuration File Descriptions

### 1. `SecurityConfig.java`
Defines global Spring Security settings.

- Enables CORS and disables CSRF
- Permits unauthenticated access to `/auth/**` and Swagger paths
- Secures all other routes
- Registers custom `JwtAuthenticationFilter` and exception handler

### 2. `RedisConfig.java`
Sets up the Redis connection and template.

- Defines a `RedisConnectionFactory`
- Registers `RedisTemplate` with string key-value pairs
- Supports JSON serialization for cached data

### 3. `S3Config.java`
Configures AWS S3 integration.

- Creates a configured `AmazonS3` client bean
- Reads AWS credentials (access key, secret key, region) from environment variables
- Used for uploading/downloading media files to/from S3

### 4. `SwaggerConfiguration.java`
Enables API documentation using Swagger UI.

- Sets metadata such as title, description, and version
- Provides UI for testing API endpoints
- Adds JWT Authorization header for secured APIs

### 5. `WebClientConfig.java`
Configures an asynchronous `WebClient` used to call the OpenAI GPT API.

- Reads the GPT API base URL from environment variables (`OPENAI_API_KEY`)
- Used to send HTTP requests to the GPT endpoint for feedback generation
- Can be extended to configure headers (Authorization), timeouts, etc.

### 6. `WebMvcConfig.java`
Customizes Spring MVC behavior.

- Configures global CORS policies
- Maps static resources (e.g., Swagger UI, static files)
- Can be extended to support locale, message converters, etc.

---

## Configuration Files Summary

| File Name                   | Purpose                                                  |
| --------------------------- | -------------------------------------------------------- |
| `SecurityConfig.java`       | Configures global security and JWT authentication        |
| `RedisConfig.java`          | Sets up Redis connection and template                    |
| `S3Config.java`             | Configures AWS S3 client for media storage               |
| `SwaggerConfiguration.java` | Enables Swagger API documentation and testing            |
| `WebClientConfig.java`      | Sets up WebClient to communicate with the OpenAI GPT API |
| `WebMvcConfig.java`         | Configures CORS and static resource handling             |



---

> These configuration files are foundational for the application's behavior. Be sure to review and test changes carefully to avoid cross-configuration conflicts.
