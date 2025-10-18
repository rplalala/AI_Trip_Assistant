# Booking Service API

This service exposes quote + confirm endpoints that power the AI Trip Assistant booking flow.

## 1. POST `/api/booking/quote`

Generate a stateless booking quote. The response contains a signed `quote_token` that encodes the pricing breakdown and expires after the configured TTL (default 15 minutes).

### Request Body

```json
{
  "product_type": "transport",
  "currency": "JPY",
  "party_size": 2,
  "params": {
    "mode": "train",
    "from": "NRT",
    "to": "Shinjuku",
    "date": "2025-11-02",
    "class": "economy"
  }
}
```

| Field | Type | Notes |
| --- | --- | --- |
| `product_type` | enum (`transport`,`hotel`,`attraction`) | Determines pricing strategy |
| `currency` | string | ISO currency code (JPY in MVP) |
| `party_size` | integer | Number of travellers |
| `params` | object | Product-specific parameters (examples below) |

#### Param Examples
- **Transport**: `{"mode":"train|flight","from":"NRT","to":"Shinjuku","date":"2025-11-02","class":"economy"}`  
- **Hotel**: `{"city":"Tokyo","check_in":"2025-11-02","check_out":"2025-11-05","stars":3,"room_type":"twin","breakfast":false}`  
- **Attraction**: `{"city":"Tokyo","name":"teamLab Planets","date":"2025-11-03","session":"10:00"}`  

### Success Response

```json
{
  "quote_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_at": "2025-10-18T04:30:00Z",
  "items": [
    {
      "sku": "HTL_TOKYO_TWIN_20251102",
      "unit_price": 12000,
      "quantity": 3,
      "fees": 1800,
      "total": 37800,
      "currency": "JPY",
      "availability": 999,
      "meta": {
        "hotel_id": "HTL_42",
        "room_type": "twin"
      },
      "cancellation_policy": "48h prior: full refund"
    }
  ]
}
```

## 2. POST `/api/booking/itinerary/quote`

Bundle multiple products into a single stateless itinerary quote. The response is a signed token that lists each itinerary item and the bundle totals. Use the item `reference` values later to confirm a subset.

### Request Body

```json
{
  "itinerary_id": "iti_tokyo_2025",
  "currency": "JPY",
  "items": [
    {
      "reference": "hotel_tokyo_nov02",
      "product_type": "hotel",
      "party_size": 2,
      "params": {
        "city": "Tokyo",
        "check_in": "2025-11-02",
        "check_out": "2025-11-05",
        "room_type": "twin"
      }
    },
    {
      "reference": "narita_transfer",
      "product_type": "transport",
      "party_size": 2,
      "params": {
        "mode": "train",
        "from": "NRT",
        "to": "Shinjuku",
        "date": "2025-11-02"
      }
    }
  ]
}
```

| Field | Type | Notes |
| --- | --- | --- |
| `itinerary_id` | string | Client-assigned identifier so the UI can correlate the quote |
| `currency` | string | Shared currency for the bundle |
| `items` | array | Minimum 1 itinerary item |
| └─ `reference` | string | Stable handle for the item (used when confirming subsets) |
| └─ `product_type` | enum | Same as single quote |
| └─ `party_size` | integer | Travellers for this item |
| └─ `params` | object | Same schema as single quote for the given product type |

### Success Response

```json
{
  "quote_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_at": "2025-10-18T04:30:00Z",
  "currency": "JPY",
  "bundle_total": 52800,
  "bundle_fees": 2100,
  "items": [
    {
      "reference": "hotel_tokyo_nov02",
      "product_type": "hotel",
      "party_size": 2,
      "total": 37800,
      "fees": 1800,
      "quote_items": [
        {
          "sku": "HTL_TOKYO_TWIN_20251102",
          "unit_price": 12000,
          "quantity": 3,
          "fees": 1800,
          "total": 37800,
          "currency": "JPY",
          "availability": 999,
          "meta": {
            "hotel_id": "HTL_42",
            "room_type": "twin"
          },
          "cancellation_policy": "48h prior: full refund"
        }
      ]
    }
  ]
}
```

## 3. POST `/api/booking/confirm`

Confirm a quote by validating the token, running mock payment, and persisting an order. Supports optional idempotency via `Idempotency-Key` header.

### Request Headers
- `Idempotency-Key` (optional UUID string)

### Request Body

```json
{
  "quote_token": "<token-from-quote>",
  "payment_token": "pm_mock_4242",
  "item_refs": ["hotel_tokyo_nov02", "narita_transfer"]
}
```

`item_refs` is optional. When omitted or empty, the service confirms every itinerary item encoded in the token. Legacy single-product quotes ignore this field.

### Success Response

```json
{
  "status": "CONFIRMED",
  "voucher_code": "VCH-7GQ2-M9K1",
  "invoice_id": "INV_5566"
}
```

## Error Responses

All errors return a JSON payload: `{"code": "ERR_CODE", "message": "Details..."}`.

| Scenario | HTTP | Code |
| --- | --- | --- |
| Quote token expired | `409 Conflict` | `ERR_QUOTE_EXPIRED` |
| Quote token invalid/signature mismatch | `409 Conflict` | `ERR_INVALID_QUOTE_TOKEN` |
| Payment declined | `402 Payment Required` | `ERR_PAYMENT_FAILED` |
| Payment token invalid | `400 Bad Request` | `ERR_PAYMENT_TOKEN` |
| Idempotency key reused with different quote | `409 Conflict` | `ERR_IDEMPOTENCY_MISMATCH` |
| Validation failure | `400 Bad Request` | `ERR_VALIDATION` |
| Other server error | `500 Internal Server Error` | `ERR_INTERNAL` |

## Pricing Notes
- Deterministic pricing per product type using request parameters as the seed – re-quoting with the same inputs gives the same price.
- Availability is mocked as `999` (no real inventory).
- Quotes are stateless; only confirmations create an order record with voucher + invoice codes.
- Itinerary tokens encode each item independently, allowing subset confirmation while guaranteeing the bundle price remains valid.
