# Map API

**Version:** v1.1  
**Base URL:** `/api/map`

The map endpoint generates routing data from Google Maps or AMap (Gaode), depending on an explicit provider override supplied by the client or (if absent) backend default selection. Responses follow the standard envelope (`code`, `msg`, `data`).

---

## Provider Selection Flow

1. Frontend may probe connectivity (e.g. loading a small Google Maps image). If probe fails it prefers `amap`, otherwise `google`.
2. Frontend sends `provider` in the request body (optional). If omitted or blank backend falls back to `google`.
3. If an unknown provider string is sent, backend gracefully defaults to `google` (no 4xx error).
4. Backend then calls the chosen provider's routing / geocoding APIs and assembles a normalized `MapRouteResponse`.

Supported providers: `google`, `amap` (enum `MapProvider`).

---

## POST `/api/map/route`
Builds a route between two locations using Google Maps (default) or AMap (Gaode) and returns summary details plus provider-specific embed/share URLs.

| Header | Value | Required | Description |
| --- | --- | --- | --- |
| `Authorization` | `Bearer <token>` | Yes | JWT access token |
| `Content-Type` | `application/json` | Yes | JSON request payload |

### Request Body
| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `origin` | string | Yes | Free-form address or `"lat,lng"` pair |
| `destination` | string | Yes | Free-form address or `"lat,lng"` pair |
| `travelMode` | string | No | Defaults to `driving`; allowed: `driving`, `walking`, `bicycling`, `transit` |
| `provider` | string | No | Override provider (`google` or `amap`). Unknown/blank → `google` |

```json
{
  "origin": "Sydney Opera House",
  "destination": "Bondi Beach",
  "travelMode": "driving"
}
```

### Success Response `data`
| Field | Type | Description |
| --- | --- | --- |
| `provider` | string | Provider actually used (`google` or `amap`) |
| `travelMode` | string | Normalized travel mode (lowercase) |
| `routeSummary` | string | Provider summary / assembled label |
| `distanceText` | string | Human-readable distance (may be `null` in fallback) |
| `distanceMeters` | integer | Distance in meters (may be `null` in fallback) |
| `durationText` | string | Human-readable duration (may be `null` in fallback) |
| `durationSeconds` | integer | Duration in seconds (may be `null` in fallback) |
| `overviewPolyline` | string | Encoded polyline (Google only; `null` for AMap currently) |
| `embedUrl` | string | Iframe URL (Google Embed API; for AMap equals share URL) or `null` when fallback for Google |
| `shareUrl` | string | Direct link to open route in provider site/app |
| `warnings` | array[string] | Combined provider warnings + internal fallback reasons |

#### Google Example
```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "provider": "google",
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

#### AMap Example
```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "provider": "amap",
    "travelMode": "walking",
    "routeSummary": "三里屯 → 首都机场",
    "distanceText": "1.5 km",
    "distanceMeters": 1500,
    "durationText": "10 min",
    "durationSeconds": 600,
    "overviewPolyline": null,
    "embedUrl": "https://uri.amap.com/navigation?callnative=0&mode=walk&coordinate=gaode&from=...",
    "shareUrl": "https://uri.amap.com/navigation?callnative=0&mode=walk&coordinate=gaode&from=...",
    "warnings": ["Route data powered by AMap (Gaode)."]
  }
}
```

#### Fallback Example (Google zero results)
```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "provider": "google",
    "travelMode": "driving",
    "routeSummary": null,
    "distanceText": null,
    "distanceMeters": null,
    "durationText": null,
    "durationSeconds": null,
    "overviewPolyline": null,
    "embedUrl": null,
    "shareUrl": "https://www.google.com/maps/dir/?api=1&origin=A&destination=B&travelmode=driving",
    "warnings": [
      "Google Maps could not provide a detailed route for this request. Showing only the share link.",
      "No routes found"
    ]
  }
}
```

### Validation & Errors
- `origin` / `destination` must be non-blank → throws `IllegalArgumentException` (mapped to `code=0` response by global handler).
- `travelMode` must be one of supported set → `Unsupported travel mode` error.
- Missing provider API key (Google or AMap) → `IllegalStateException` with message e.g. `Google Maps API key is not configured`.
- Network / provider error → fallback attempt; if impossible still returns `code=0` with error message.

Example error:
```json
{ "code": 0, "msg": "Unsupported travel mode: flying", "data": null }
```

### Backend vs Frontend URLs
| Purpose | URL | Who initiates | Notes |
| --- | --- | --- | --- |
| Directions (Google) | `https://maps.googleapis.com/maps/api/directions/json` | Backend | Returns structured JSON (needs server key) |
| Geocode (Google) | `https://maps.googleapis.com/maps/api/geocode/json` | Backend | Optional, improves AMap route via WGS84 → GCJ-02 conversion |
| Geocode (AMap) | `https://restapi.amap.com/v3/geocode/geo` | Backend | Fallback when Google geocode unavailable |
| Driving (AMap) | `https://restapi.amap.com/v3/direction/driving` | Backend | Uses GCJ-02 coordinates |
| Walking (AMap) | `https://restapi.amap.com/v3/direction/walking` | Backend |  |
| Transit (AMap) | `https://restapi.amap.com/v3/direction/transit/integrated` | Backend | City param may be set |
| Bicycling (AMap) | `https://restapi.amap.com/v4/direction/bicycling` | Backend |  |
| Embed (Google) | `https://www.google.com/maps/embed/v1/directions` | Browser (iframe) | Key visible client-side; restrict by domain & API scope |
| Share (Google) | `https://www.google.com/maps/dir/` | Browser (link) | No server call |
| Share/Embed (AMap) | `https://uri.amap.com/navigation` | Browser (link/iframe) | Same URL used for share & embed currently |

---

## Configuration

| Property | Environment variable | Description |
| --- | --- | --- |
| `google.maps.api-key` | `GOOGLE_MAPS_API_KEY` | Server-side key for Directions + Embed (consider splitting to separate limited keys) |
| `amap.web-service.api-key` | `AMAP_WEB_SERVICE_API_KEY` | Backend Web Service key (restrict by server IP) |

Frontend build-time variables:
| Variable | Description |
| --- | --- |
| `VITE_AMAP_JS_API_KEY` | AMap JS API key (domain whitelist) |

### Security Recommendations
- Use separate restricted Google keys: one for server (IP restricted, Directions/Geocode only) and one for embed (HTTP referrer restricted, Embed API only). Current implementation uses a single key; splitting reduces exposure.
- Never expose `amap.web-service.api-key` to the browser; only share the JS API key meant for front-end maps (if interactive maps are later added).
- Log provider errors at WARN/ERROR but avoid leaking full upstream error objects to clients.
- Consider rate limiting by origin/destination pair to mitigate abuse.

### Polyline Decoding (Optional)
`overviewPolyline` is an encoded polyline string from Google. Frontend may decode using standard polyline algorithm to draw a custom map if needed. (AMap directions currently return no polyline; enhancement possible by aggregating step coordinates.)

### Future Enhancements
- Provide endpoint `/api/map/providers` to enumerate supported providers and modes.
- Add AMap polyline extraction (requires parsing steps array). 
- Cache frequent geocode + directions queries (short TTL) to reduce external API costs.

---

Last updated: 2025-11-08
