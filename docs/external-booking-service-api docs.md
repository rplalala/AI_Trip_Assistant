# External Booking Service (Mock) API (Backend-Facing)

**Version:** v1.0  
**Base URL:** `/api`

These endpoints are consumed by the AI Trip Assistant backend via Feign clients. Responses are raw JSON objects (no `code/msg` envelope) and the service does not require authentication.

---

## POST `/api/booking/quote`
Calculates pricing for a single itinerary item and returns voucher/invoice identifiers with detailed line items.

| Header | Value | Required | Description |
| --- | --- | --- | --- |
| `Content-Type` | `application/json` | Yes | JSON request payload |

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `product_type` | enum | Yes | `transport`, `hotel`, or `attraction` |
| `currency` | string | Yes | ISO currency code |
| `party_size` | integer | Yes | Number of travellers |
| `params` | object | Yes | Product-specific parameters (dates, provider, price, etc.) |
| `trip_id` | number | No | Upstream trip identifier passed through from the API module |
| `entity_id` | number | No | Upstream entity identifier passed through from the API module |

```json
{
  "product_type": "transport",
  "currency": "AUD",
  "party_size": 2,
  "params": {
    "from": "Sydney, Australia",
    "to": "Tokyo, Japan",
    "date": "2025-11-02",
    "time": "10:00",
    "provider": "Qantas",
    "ticket_type": "economy",
    "price": 1000
  },
  "trip_id": 1,
  "entity_id": 1
}
```

**Response body**

| Field | Type | Description |
| --- | --- | --- |
| `voucher_code` | string | Generated voucher reference |
| `invoice_id` | string | Generated invoice reference |
| `items` | array | Detailed quote line items |

Each item includes `sku`, `unit_price`, `quantity`, `fees`, `total`, `currency`, `meta`, and `cancellation_policy`.

---

## POST `/api/booking/itinerary/quote`
Bundles multiple products into a single itinerary quote, aggregating totals while preserving per-item pricing.

| Header | Value | Required | Description |
| --- | --- | --- | --- |
| `Content-Type` | `application/json` | Yes | JSON request payload |

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `itinerary_id` | string | Yes | Correlation ID used by the caller |
| `currency` | string | Yes | Shared currency |
| `items` | array | Yes | At least one itinerary item |
| `items[].reference` | string | Yes | Stable handle for the item |
| `items[].product_type` | enum | Yes | `transport`, `hotel`, or `attraction` |
| `items[].party_size` | integer | Yes | Travellers for this item |
| `items[].params` | object | Yes | Product-specific parameters |
| `items[].entity_id` | number | No | Upstream entity identifier |
| `trip_id` | number | No | Upstream trip identifier applied to the bundle |

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

**Response body**

| Field | Type | Description |
| --- | --- | --- |
| `voucher_code` | string | Voucher reference for the bundle |
| `invoice_id` | string | Invoice reference for the bundle |
| `currency` | string | Currency applied to all items |
| `items` | array | Per-item results containing `reference`, `product_type`, `party_size`, `total`, `fees`, and `quote_items` |
| `bundle_total` | number | Sum of all item totals |
| `bundle_fees` | number | Aggregate fees |

Each `quote_items` entry mirrors the single-quote item schema.

---

## GET `/api/booking/ping`
Simple health endpoint used by the API module to verify connectivity.

No parameters are required. Example response:

```json
{
  "ok": true,
  "service": "external-service",
  "random": 0.4837263725
}
```

