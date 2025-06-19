# Utility Class Descriptions

This section describes classes located in the `utils` package. These utility classes assist in operations such as multipart handling or resource abstraction.

---

## MultipartInputStreamFileResource

**File:** `MultipartInputStreamFileResource.java`

**Description:**
A utility class that wraps an `InputStream` as a Spring `FileSystemResource`, suitable for use in `RestTemplate` or other multipart upload scenarios.

**Primary Use Case:**

* Enables `InputStream`-based files (e.g., dynamically generated content or streams) to be uploaded as if they were actual files

**Key Methods & Fields:**

* `getFilename()`: Returns the specified file name for multipart upload
* `contentLength()`: Returns the stream size or throws `IOException` if not determinable

---

## FrameIndexCalculator *(Not in Use)*

**File:** `FrameIndexCalculator.java`

**Description:**
Originally intended to calculate frame indices based on timestamps and FPS, but currently not used in the project.

**Status:** Not actively used in production or core logic. Safe to exclude from documentation or remove unless planned for future use.

**Note:** This class might have been part of an early prototype or utility abstraction but has since been deprecated.
