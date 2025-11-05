# Map API

**Version:** v1.0  
**Base URL:** `/api/map`

The map endpoint generates Google Directions data that the frontend uses to render routes and provide shareable links. Responses follow the standard envelope (`code`, `msg`, `data`).

---

## POST `/api/map/route`
Builds a route between two locations using the Google Directions API and returns summary details plus embed/share URLs.

| Header | Value | Required | Description |
| --- | --- | --- | --- |
| `Authorization` | `Bearer <token>` | Yes | JWT access token |
| `Content-Type` | `application/json` | Yes | JSON request payload |

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `origin` | string | Yes | Free-form address or `"lat,lng"` pair |
| `destination` | string | Yes | Free-form address or `"lat,lng"` pair |
| `travelMode` | string | No | Defaults to `driving`; allowed values: `driving`, `walking`, `bicycling`, `transit` |

```json
{
  "origin": "Sydney Opera House",
  "destination": "Bondi Beach",
  "travelMode": "driving"
}
```

**Success response (`data`)**

| Field | Type | Description |
| --- | --- | --- |
| `travelMode` | string | Normalized travel mode sent to Google |
| `routeSummary` | string | Google-provided summary (major road or instruction) |
| `distanceText` | string | Human-readable distance |
| `distanceMeters` | integer | Distance in meters |
| `durationText` | string | Human-readable duration |
| `durationSeconds` | integer | Duration in seconds |
| `overviewPolyline` | string | Encoded polyline for client-side map rendering |
| `embedUrl` | string | URL for the Google Maps Embed API |
| `shareUrl` | string | Link that opens the same route in Google Maps |
| `warnings` | array[string] | Any warnings returned by Google (may be empty) |

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "travelMode": "driving",
    "routeSummary": "New South Head Rd",
    "distanceText": "7.0 km",
    "distanceMeters": 7040,
    "durationText": "16 mins",
    "durationSeconds": 960,
    "overviewPolyline": "dzhdEierx...h~cA",
    "embedUrl": "https://www.google.com/maps/embed/v1/directions?key=***&origin=Sydney+Opera+House&destination=Bondi+Beach&mode=driving",
    "shareUrl": "https://www.google.com/maps/dir/?api=1&origin=Sydney+Opera+House&destination=Bondi+Beach&travelmode=driving",
    "warnings": []
  }
}
```

---

## Configuration

| Property | Environment variable | Description |
| --- | --- | --- |
| `google.maps.api-key` | `GOOGLE_MAPS_API_KEY` | Google Maps Platform key with Directions API and Maps Embed API enabled |

Provide the key in `api/src/main/resources/application.yaml` or via environment configuration. Without a valid key the endpoint returns an error from Google.

