# Trip API

**Version:** v1.0  
**Base URL:** `/api/trip`

All endpoints require `Authorization: Bearer <token>` and return the standard envelope (`code`, `msg`, `data`). Trip generation requests run asynchronously: the API responds immediately while itinerary generation continues in the background. Use the details/timeline endpoints to fetch persisted results once ready.

---

## POST `/api/trip/generate-plan`
Creates a new trip based on the submitted preferences and kicks off itinerary generation.

| Header | Value | Required | Description |
| --- | --- | --- | --- |
| `Authorization` | `Bearer <token>` | Yes | JWT access token |
| `Content-Type` | `application/json` | Yes | JSON request payload |

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `fromCountry` | string | No | Departure country |
| `fromCity` | string | No | Departure city |
| `toCountry` | string | Yes | Destination country |
| `toCity` | string | Yes | Destination city |
| `currency` | string | Yes | Budget currency code |
| `budget` | integer | No | Maximum spend |
| `people` | integer | No | Number of travellers |
| `startDate` | string (ISO date) | No | Trip start date |
| `endDate` | string (ISO date) | No | Trip end date |
| `preferences` | string | No | Free-form preferences passed to AI planning |

```json
{
  "fromCountry": "Australia",
  "fromCity": "Sydney",
  "toCountry": "Japan",
  "toCity": "Tokyo",
  "currency": "JPY",
  "budget": 600000,
  "people": 2,
  "startDate": "2025-10-28",
  "endDate": "2025-11-04",
  "preferences": "Focus on cultural attractions and food."
}
```

Response payload: `data` is `null` on acceptance.

---

## POST `/api/trip/regenerate-plan`
Rebuilds an existing trip using updated user feedback or preferences.

| Header | Value | Required | Description |
| --- | --- | --- | --- |
| `Authorization` | `Bearer <token>` | Yes | JWT access token |
| `Content-Type` | `application/json` | Yes | JSON request payload |

| Query Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| `tripId` | number | Yes | Trip identifier to regenerate |

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `secondPreference` | string | No | Additional notes that influence regeneration |

```http
POST /api/trip/regenerate-plan?tripId=42 HTTP/1.1
Authorization: Bearer <token>
Content-Type: application/json

{
  "secondPreference": "Add a day trip to Nikko and more family-friendly dining."
}
```

Response payload: `data` is `null` on acceptance.

---

## GET `/api/trip/details`
Returns the list of trips owned by the authenticated user, including basic metadata used in the trip dashboard.

| Header | Value | Required | Description |
| --- | --- | --- | --- |
| `Authorization` | `Bearer <token>` | Yes | JWT access token |

**Success response (`data`)**: array of trip detail objects.

| Field | Type | Description |
| --- | --- | --- |
| `tripId` | number | Trip identifier |
| `fromCountry` | string | Departure country |
| `fromCity` | string | Departure city |
| `toCountry` | string | Destination country |
| `toCity` | string | Destination city |
| `budget` | integer | Stored budget |
| `people` | integer | Number of travellers |
| `startDate` | string | Start date (ISO-8601) |
| `endDate` | string | End date (ISO-8601) |
| `imgUrl` | string | Hero image URL, when available |

---

## GET `/api/trip/insights`
Retrieves AI-generated insights for the specified trip. Generates and caches insights on first request.

| Header | Value | Required | Description |
| --- | --- | --- | --- |
| `Authorization` | `Bearer <token>` | Yes | JWT access token |

| Query Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| `tripId` | number | Yes | Trip identifier |

**Success response (`data`)**: array of insights.

| Field | Type | Description |
| --- | --- | --- |
| `id` | string | Stable identifier for the insight card |
| `title` | string | Heading used in the UI |
| `content` | string | Insight body |
| `theme` | string | Optional theme tag (e.g. `food`, `weather`) |
| `icon` | string | Icon name to render client-side |

---

## GET `/api/trip/timeline`
Returns the day-by-day itinerary extracted from the generated trip.

| Header | Value | Required | Description |
| --- | --- | --- | --- |
| `Authorization` | `Bearer <token>` | Yes | JWT access token |

| Query Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| `tripId` | number | Yes | Trip identifier |

**Success response (`data`)**: array of timeline entries.

| Field | Type | Description |
| --- | --- | --- |
| `date` | string | Itinerary date |
| `imageUrl` | string | Representative image for the day |
| `summary` | string | Highlight summary |
| `maxTemperature` | number | Forecasted max temperature |
| `minTemperature` | number | Forecasted min temperature |
| `weatherCondition` | string | Weather description |
| `attraction` | array | List of attraction items (`location`, `time`, `title`) |
| `hotel` | array | List of hotel items (`hotelName`, `time`, `title`) |
| `transportation` | array | List of transport items (`time`, `title`, `from`, `to`) |

---

## DELETE `/api/trip`
Deletes one or more trips owned by the authenticated user.

| Header | Value | Required | Description |
| --- | --- | --- | --- |
| `Authorization` | `Bearer <token>` | Yes | JWT access token |

| Query Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| `tripIds` | array[number] | Yes | Repeatable query parameter containing trip IDs to remove |

Example request: `DELETE /api/trip?tripIds=12&tripIds=19`.

Response payload: `data` is `null` on success.

