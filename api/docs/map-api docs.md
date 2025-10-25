# Map API Docs

**API Version:** v1.0

Backend endpoint that produces Google Maps routes consumable by the frontend “Generate map” button.

---
## 1. Generate Route

> BASIC

**Path:** `/api/map/route`  
**Method:** `POST`  
**Auth:** Requires `Authorization: Bearer <JWT>` (same as other secure APIs)  
**Desc:** Calls Google Directions API to build a route and returns metadata including embed/share links.

> REQUEST

**Headers**

| name | value | required | description |
| --- | --- | --- | --- |
| Authorization | Bearer `<jwt>` | YES | JWT obtained during login |
| Content-Type | application/json | YES | — |

**Body**

| name | type | required | description |
| --- | --- | --- | --- |
| origin | string | YES | Free-form address or `lat,lng` pair |
| destination | string | YES | Free-form address or `lat,lng` pair |
| travelMode | string | NO | Defaults to `driving`. Allowed: `driving`, `walking`, `bicycling`, `transit` |

**Request Example**

```json
{
  "origin": "Sydney Opera House",
  "destination": "Bondi Beach",
  "travelMode": "driving"
}
```

> RESPONSE

**Headers**

| name | value | required | description |
| --- | --- | --- | --- |
| content-type | application/json;charset=UTF-8 | YES | — |

**Body**

| name | type | description |
| --- | --- | --- |
| code | integer | 1 success; 0 failure |
| msg | string | success or error message |
| data | object | Route payload |
| └─ travelMode | string | Normalized travel mode sent to Google |
| └─ routeSummary | string | Google-provided summary (e.g. main highway) |
| └─ distanceText | string | Human-readable distance |
| └─ distanceMeters | integer | Distance in meters |
| └─ durationText | string | Human-readable travel time |
| └─ durationSeconds | integer | Travel time in seconds |
| └─ overviewPolyline | string | Encoded polyline for drawing the path client-side |
| └─ embedUrl | string | URL for a Google Maps iframe |
| └─ shareUrl | string | Link that opens the same route in Google Maps |
| └─ warnings | array[string] | Any warnings returned by Google (may be empty) |

**Response Example**

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
    "overviewPolyline": "dzhdEierx[...]h~cA",
    "embedUrl": "https://www.google.com/maps/embed/v1/directions?key=***&origin=Sydney+Opera+House&destination=Bondi+Beach&mode=driving",
    "shareUrl": "https://www.google.com/maps/dir/?api=1&origin=Sydney+Opera+House&destination=Bondi+Beach&travelmode=driving",
    "warnings": []
  }
}
```

---
### Configuration

| property | env var | description |
| --- | --- | --- |
| `google.maps.api-key` | `GOOGLE_MAPS_API_KEY` | Google Maps Platform API key with “Directions API” + “Maps Embed API” enabled |

Add the key to `api/src/main/resources/application.yaml` or provide it through the environment. Without a key the endpoint returns an error.

