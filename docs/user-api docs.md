# Auth & Profile & Media Upload API

**Version:** v1.0  
**Base URL:** `/api`

All endpoints return the standard response envelope:
- `code`: integer (`1` for success, `0` for failure)
- `msg`: message string (`success` or a human-readable error)
- `data`: endpoint-specific payload (may be `null`)

Unless noted, requests and responses are JSON encoded in UTF-8.

---

## Authentication

### POST `/api/login`
Authenticates a user by email and password. Returns a JWT access token that must be supplied in the `Authorization: Bearer <token>` header for protected endpoints.

| Header | Value | Required | Description |
| --- | --- | --- | --- |
| `Content-Type` | `application/json` | Yes | JSON request payload |

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `email` | string | Yes | Registered email address |
| `password` | string | Yes | Password (6–64 characters) |

```json
{
  "email": "jessica@example.com",
  "password": "sup3rSecure!"
}
```

**Success response**

```json
{
  "code": 1,
  "msg": "success",
  "data": "eyJhbGciOiJIUzI1NiJ9..."
}
```

---

### POST `/api/register`
Creates a new account. On success no token is issued automatically; call `/api/login` to authenticate.

| Header | Value | Required | Description |
| --- | --- | --- | --- |
| `Content-Type` | `application/json` | Yes | JSON request payload |

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `username` | string | Yes | 3–30 characters; letters, numbers, `_`, and `-` |
| `email` | string | Yes | Unique email address |
| `password` | string | Yes | Password (6–64 characters) |

```json
{
  "username": "travel-lover",
  "email": "traveller@example.com",
  "password": "sup3rSecure!"
}
```

---

### GET `/api/verify-email`
Validates an email verification token sent via email and returns a fresh JWT when successful. Include the token as a query parameter.

| Query Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| `token` | string | Yes | Verification token from the email link |

```http
GET /api/verify-email?token=eyJhbGciOi... HTTP/1.1
```

**Success response (`data`)**: JWT access token string.

---

### POST `/api/resend-verify-email`
Sends a new verification email to the specified address. Use when a user attempted to log in before verifying their email.

| Query Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| `email` | string | Yes | Registered email address |

```http
POST /api/resend-verify-email?email=traveller@example.com HTTP/1.1
```

Returns the standard envelope with `data: null` on success.

---

### POST `/api/forgot-password`
Initiates password recovery by emailing a reset link to the user.

| Query Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| `email` | string | Yes | Registered email address |

```http
POST /api/forgot-password?email=traveller@example.com HTTP/1.1
```

Returns the standard envelope with `data: null` on success.

---

### GET `/api/verify-reset-password-email`
Validates the reset-password token from the email link before displaying the reset form.

| Query Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| `token` | string | Yes | Reset token from the email |

```http
GET /api/verify-reset-password-email?token=eyJhbGciOi... HTTP/1.1
```

Returns the standard envelope with `data: null` when the token is valid; otherwise an error is returned.

---

### POST `/api/reset-password`
Completes password reset using a valid token and a new password provided by the user.

| Query Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| `token` | string | Yes | Reset token from the email link |
| `newPassword` | string | Yes | New password (6–64 characters) |

```http
POST /api/reset-password?token=eyJhbGciOi...&newPassword=NewSecurePwd1 HTTP/1.1
```

Returns the standard envelope with `data: null` on success.

---

## User Profile

All profile endpoints require `Authorization: Bearer <token>`. The `userId` is resolved from the JWT; no query parameters are required.

### GET `/api/users/profile`
Returns the authenticated user profile.

| Field | Type | Description |
| --- | --- | --- |
| `username` | string | Display name; may be `null` |
| `age` | integer | Age between 0 and 150; may be `null` |
| `gender` | integer | `1` male, `2` female; may be `null` |
| `email` | string | Registered email (read-only) |
| `avatar` | string | HTTPS URL of the current avatar |

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "username": "travel-lover",
    "age": 29,
    "gender": 2,
    "email": "traveller@example.com",
    "avatar": "https://cdn.example.com/avatars/default.png"
  }
}
```

---

### PUT `/api/users/profile`
Updates basic profile attributes. Provide only the fields you want to change.

| Header | Value | Required | Description |
| --- | --- | --- | --- |
| `Authorization` | `Bearer <token>` | Yes | JWT access token |
| `Content-Type` | `application/json` | Yes | JSON request payload |

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `username` | string | No | 3–30 characters; letters, numbers, `_`, and `-` |
| `age` | integer | No | Age between 0 and 150 |
| `gender` | integer | No | `1` male, `2` female |

```json
{
  "username": "travel-pro",
  "age": 30,
  "gender": 1
}
```

Returns the standard envelope with `data: null` on success.

---

### PUT `/api/users/profile/pd`
Changes the account password. The current token becomes invalid after the change; log in again to obtain a fresh JWT.

| Header | Value | Required | Description |
| --- | --- | --- | --- |
| `Authorization` | `Bearer <token>` | Yes | JWT access token |
| `Content-Type` | `application/json` | Yes | JSON request payload |

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `oldPassword` | string | Yes | Existing password (6–64 characters) |
| `newPassword` | string | Yes | New password (6–64 characters) |

```json
{
  "oldPassword": "sup3rSecure!",
  "newPassword": "anEvenBetterOne1"
}
```

---

### DELETE `/api/users/profile`
Deletes the authenticated user account after verifying their password. This action removes associated trip data.

| Header | Value | Required | Description |
| --- | --- | --- | --- |
| `Authorization` | `Bearer <token>` | Yes | JWT access token |
| `Content-Type` | `application/json` | Yes | JSON request payload |

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `verifyPassword` | string | Yes | Current password used to confirm deletion |

```json
{
  "verifyPassword": "sup3rSecure!"
}
```

Returns the standard envelope with `data: null` on success.

---

### POST `/api/users/profile/change-email-link`
Sends a change-email confirmation link to the user’s current email address.

| Header | Value | Required | Description |
| --- | --- | --- | --- |
| `Authorization` | `Bearer <token>` | Yes | JWT access token |

No request body is required. Returns the standard envelope with `data: null` on success.

---

### POST `/api/users/profile/change-email`
Accepts a change-email token (from the link sent to the existing email) and the new email address the user entered in the confirmation form.

| Query Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| `token` | string | Yes | Token from the change-email link in the old email |
| `newEmail` | string | Yes | Email address to switch to |

```http
POST /api/users/profile/change-email?token=eyJhbGciOi...&newEmail=new@example.com HTTP/1.1
```

Returns the standard envelope with `data: null` on success.

---

### GET `/api/users/profile/verify-change-email-token`
Validates the token before displaying the new email entry form.

| Query Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| `token` | string | Yes | Token from the change-email link in the old email |

Returns the standard envelope with `data: null` when valid.

---

### GET `/api/users/profile/confirm-change-email`
Completes the change-email flow after the user clicks the confirmation link sent to the new email address.

| Query Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| `token` | string | Yes | Token from the confirmation link in the new email |

Returns the standard envelope with `data: null` on success.

---

## Media Upload

All upload endpoints require `Authorization: Bearer <token>`. Supported formats: `jpg`, `jpeg`, `png`, `gif`, `svg`, `webp`. Maximum file size is 10 MB unless configured otherwise.

### POST `/api/upload`
Uploads an image to AWS S3 and returns the generated URL. Use for generic content uploads.

| Header | Value | Required | Description |
| --- | --- | --- | --- |
| `Authorization` | `Bearer <token>` | Yes | JWT access token |
| `Content-Type` | `multipart/form-data` | Yes | Form-data upload |

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `file` | file | Yes | Image file to upload |

```json
{
  "code": 1,
  "msg": "success",
  "data": "https://cdn.example.com/uploads/2025/10/sample.png"
}
```

---

### POST `/api/upload/avatar`
Uploads an avatar image and immediately updates the authenticated user profile.

| Header | Value | Required | Description |
| --- | --- | --- | --- |
| `Authorization` | `Bearer <token>` | Yes | JWT access token |
| `Content-Type` | `multipart/form-data` | Yes | Form-data upload |

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `file` | file | Yes | Avatar image to upload |

```json
{
  "code": 1,
  "msg": "success",
  "data": "https://cdn.example.com/avatars/user-42.png"
}
```

---

### POST `/api/upload/avatar/link`
Fetches an external image, stores it as the user’s avatar, and updates the profile.

| Header | Value | Required | Description |
| --- | --- | --- | --- |
| `Authorization` | `Bearer <token>` | Yes | JWT access token |

| Query Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| `url` | string | Yes | Public image URL to fetch |

Returns the standard envelope with the hosted avatar URL in `data`.

---

### POST `/api/upload/link`
Fetches an external image by URL, stores it in S3, and returns the hosted link.

| Header | Value | Required | Description |
| --- | --- | --- | --- |
| `Authorization` | `Bearer <token>` | Yes | JWT access token |

| Query Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| `url` | string | Yes | Publicly accessible image URL |

The response follows the standard envelope with `data` containing the S3 URL of the stored image.
