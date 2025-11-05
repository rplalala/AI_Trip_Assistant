# Booking API (Frontend-Facing)

**Version:** v1.0  
**Base URL:** `/api/booking`

All endpoints require `Authorization: Bearer <token>` unless stated otherwise. Responses use the common envelope: `code` (1 success, 0 failure), `msg`, and `data`.

---

## Integration Overview

**Server workflow**
- Incoming REST calls are handled by `BookingController`, which delegates to `BookingFacade` for validation, enrichment, and orchestration.
- `BookingFacade` and `BookingServiceImpl` reuse the shared Feign `BookingClient` that talks to the `external-service` booking provider.
- Before calling the external service, the facade enriches payloads using repositories such as `TripHotelRepository`, `TripTransportationRepository`, and `TripAttractionRepository` based on `trip_id` and `entity_id`.
- Successful responses are persisted through `TripBookingQuoteRepository`, and associated trip entities are moved to `confirm` status.

**Frontend workflow**
- The UI invokes `/api/booking/quote` for single products and `/api/booking/itinerary/quote` for bundles created during itinerary building.
- Requests must include `trip_id`, `entity_id`, and `item_reference` so the backend can hydrate payloads and reconcile results with itinerary cards.
- Voucher, invoice, and pricing results are reflected back in the itinerary UI immediately after the quote call completes.

**Database usage**
- Pricing metadata is sourced from `trip_transportation`, `trip_hotel`, and `trip_attraction`.
- Confirmed quotes are stored in `trip_booking_quote`, linking vouchers/invoices back to their originating trip items.
- Status transitions are written to the underlying trip tables to ensure the user sees confirmed items in subsequent queries.

---

## Endpoints

### POST `/api/booking/quote`
Generates a quote for a single itinerary item and returns voucher and invoice identifiers plus detailed pricing lines.

| Header | Value | Required | Description |
| --- | --- | --- | --- |
| `Authorization` | `Bearer <token>` | Yes | JWT access token |
| `Content-Type` | `application/json` | Yes | JSON payload |

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `product_type` | enum | Yes | `transport`, `hotel`, or `attraction` |
| `currency` | string | Yes | ISO currency code (e.g. `JPY`) |
| `party_size` | integer | Yes | Number of travellers |
| `params` | object | Yes | Product-specific inputs (dates, names, prices, etc.) |
| `trip_id` | number | Yes | Trip identifier used for persistence and status updates |
| `entity_id` | number | Yes | Trip entity identifier matched to stored itinerary data |
| `item_reference` | string | Conditional | Stable reference used to map quotes back to itinerary cards; required when quoting an existing itinerary item |

```json
{
  "product_type": "hotel",
  "currency": "AUD",
  "party_size": 3,
  "params": {
    "city": "Beijing",
    "check_in": "2025-10-30",
    "nights": 4,
    "time": "15:00",
    "status": "pending",
    "room_type": "Triple room",
    "price": 800
  },
  "trip_id": 2,
  "entity_id": 4,
  "item_reference": "hotel_4"
}
```

**Success response (`data`)**

| Field | Type | Description |
| --- | --- | --- |
| `voucher_code` | string | Voucher created by the booking provider |
| `invoice_id` | string | Invoice reference |
| `items` | array | Quoted line items |

Each item contains `sku`, `unit_price`, `quantity`, `fees`, `total`, `currency`, `meta` (map of supplemental fields), and `cancellation_policy`.

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "voucher_code": "VCH-0137-0E0E",
    "invoice_id": "INV_8487",
    "items": [
      {
        "sku": "HTL_BEIJING_CITY_TRIPLE_ROOM_2025-10-30",
        "unit_price": 800,
        "quantity": 1,
        "fees": 0,
        "total": 800,
        "currency": "AUD",
        "meta": {
          "title": "Check-in at City Hotel",
          "nights": 4,
          "room_type": "Triple room",
          "time": "15:00",
          "people": 3,
          "status": "pending"
        },
        "cancellation_policy": "Full refund until 48h prior."
      }
    ]
  }
}
```

---

### GET `/api/booking`
Lists reservation-required items for a given trip, including any quote payloads needed to confirm them and recent quote summaries. This powers the booking panel in the web app.

| Header | Value | Required | Description |
| --- | --- | --- | --- |
| `Authorization` | `Bearer <token>` | Yes | JWT access token |

| Query Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| `tripId` | number | Yes | Trip identifier whose items should be listed |

**Success response (`data`)**: array of booking items with the following shape.

| Field | Type | Description |
| --- | --- | --- |
| `entityId` | number | Trip entity identifier (e.g. hotel/transport record) |
| `tripId` | number | Trip identifier |
| `productType` | string | `transport`, `hotel`, or `attraction` |
| `title` | string | Display name for the itinerary card |
| `subtitle` | string | Supplemental label (e.g. provider) |
| `date` | string | ISO-8601 date for the activity |
| `time` | string | Start time |
| `status` | string | Current status such as `pending` or `confirm` |
| `reservationRequired` | boolean | Indicates whether confirmation is required |
| `price` | integer | Stored price for quick reference |
| `currency` | string | Currency code |
| `imageUrl` | string | Preview image URL, when available |
| `metadata` | object | Additional itinerary data keyed by attribute |
| `quoteRequest` | object | Pre-built payload to send to `/api/booking/quote` (fields mirror the request body) |
| `quoteSummary` | object | Most recent quote snapshot including voucher/invoice identifiers |

```json
{
  "code": 1,
  "msg": "success",
  "data": [
    {
      "entityId": 12,
      "tripId": 7,
      "productType": "transport",
      "title": "Flight to Tokyo",
      "subtitle": "Qantas QF25",
      "date": "2025-10-27",
      "time": "10:00",
      "status": "pending",
      "reservationRequired": true,
      "price": 1000,
      "currency": "AUD",
      "imageUrl": "https://cdn.example.com/images/qf25.png",
      "metadata": {
        "from": "Sydney, Australia",
        "to": "Tokyo, Japan"
      },
      "quoteRequest": {
        "productType": "transport",
        "currency": "AUD",
        "partySize": 2,
        "params": {
          "from": "Sydney, Australia",
          "to": "Tokyo, Japan",
          "date": "2025-10-27",
          "time": "10:00",
          "provider": "Qantas",
          "ticket_type": "economy",
          "price": 1000
        },
        "tripId": 7,
        "entityId": 12,
        "itemReference": "transport_qf25"
      },
      "quoteSummary": {
        "voucherCode": "VCH-7428-086D",
        "invoiceId": "INV_2667",
        "status": "confirm",
        "currency": "AUD",
        "totalAmount": 1000
      }
    }
  ]
}
```

---

### POST `/api/booking/itinerary/quote`
Bundles multiple itinerary items into a single quote, returning combined totals alongside per-item pricing.

| Header | Value | Required | Description |
| --- | --- | --- | --- |
| `Authorization` | `Bearer <token>` | Yes | JWT access token |
| `Content-Type` | `application/json` | Yes | JSON payload |

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `itinerary_id` | string | Yes | Client-side identifier for the itinerary draft |
| `currency` | string | Yes | Shared currency for the bundle |
| `trip_id` | number | Yes | Trip identifier applied to all items |
| `items` | array | Yes | Array of itinerary entries (minimum one) |
| `items[].reference` | string | Yes | Stable identifier to reconcile UI cards |
| `items[].product_type` | enum | Yes | `transport`, `hotel`, or `attraction` |
| `items[].party_size` | integer | Yes | Travellers for the item |
| `items[].entity_id` | number | Yes | Trip entity identifier for data enrichment |
| `items[].params` | object | Yes | Product-specific parameters, mirroring the single quote payload |

```json
{
  "itinerary_id": "iti_tokyo",
  "currency": "JPY",
  "trip_id": 1,
  "items": [
    {
      "reference": "hotel_tokyo_stay",
      "product_type": "hotel",
      "party_size": 2,
      "entity_id": 2,
      "params": {
        "date": "2025-10-28",
        "time": "15:00",
        "title": "Check-in at Tokyo Hotel",
        "hotel_name": "Tokyo City Hotel",
        "room_type": "Double room",
        "nights": 6,
        "people": 2,
        "price": 1200,
        "fees": 0
      }
    },
    {
      "reference": "transport_back_to_sydney",
      "product_type": "transport",
      "party_size": 2,
      "entity_id": 3,
      "params": {
        "from": "Tokyo, Japan",
        "to": "Sydney, Australia",
        "date": "2025-11-04",
        "time": "12:00",
        "provider": "Qantas",
        "ticket_type": "economy",
        "people": 2,
        "price": 1000
      }
    }
  ]
}
```

**Success response (`data`)**

| Field | Type | Description |
| --- | --- | --- |
| `voucher_code` | string | Voucher for the itinerary |
| `invoice_id` | string | Invoice for the bundle |
| `currency` | string | Currency applied to totals |
| `bundle_total` | number | Sum of all item totals |
| `bundle_fees` | number | Sum of all item fees |
| `items` | array | Per-item pricing results |

Each item mirrors the single-quote schema and includes `quote_items`, an array of detailed line items.

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "voucher_code": "VCH-C7D2-9A3E",
    "invoice_id": "INV_2847",
    "currency": "JPY",
    "bundle_total": 2200,
    "bundle_fees": 0,
    "items": [
      {
        "reference": "hotel_tokyo_stay",
        "product_type": "hotel",
        "party_size": 2,
        "total": 1200,
        "fees": 0,
        "quote_items": [
          {
            "sku": "HTL_TOKYO_CITY_HOTEL_DOUBLE_ROOM_2025-10-28",
            "unit_price": 1200,
            "quantity": 1,
            "fees": 0,
            "total": 1200,
            "currency": "JPY",
            "meta": {
              "nights": 6,
              "date": "2025-10-28",
              "room_type": "Double room",
              "time": "15:00",
              "title": "Check-in at Tokyo Hotel",
              "reservation_required": true,
              "hotel_name": "Tokyo City Hotel",
              "people": 2,
              "status": "pending"
            },
            "cancellation_policy": "48h prior: full refund"
          }
        ]
      },
      {
        "reference": "transport_back_to_sydney",
        "product_type": "transport",
        "party_size": 2,
        "total": 1000,
        "fees": 0,
        "quote_items": [
          {
            "sku": "TP_TOKYO_JAPAN_SYDNEY_AUSTRALIA_2025-11-04",
            "unit_price": 1000,
            "quantity": 1,
            "fees": 0,
            "total": 1000,
            "currency": "JPY",
            "meta": {
              "date": "2025-11-04",
              "provider": "Qantas",
              "mode": "transport",
              "status": "pending",
              "to": "Sydney, Australia",
              "from": "Tokyo, Japan",
              "reservation_required": true,
              "ticket_type": "economy",
              "time": "12:00",
              "people": 2
            },
            "cancellation_policy": "No charge until 7 days prior; 25% after."
          }
        ]
      }
    ]
  }
}
```

---

### Error Codes

| HTTP Status | `code` | Description |
| --- | --- | --- |
| 400 | `ERR_VALIDATION` | Request failed validation or required identifiers were missing |
| 500 | `ERR_INTERNAL` | Unexpected server error while calling the external provider |

---

## Additional Notes

- Pricing is deterministic per product type; re-quoting identical payloads yields the same totals.
- Voucher and invoice identifiers are mock values suitable for demos and integration testing.
- The booking APIs do not send emails or notifications; downstream systems are expected to use the returned identifiers as confirmations.
