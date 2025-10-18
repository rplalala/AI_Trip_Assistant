# Booking API Docs

**API Version:** v1.0  

All booking endpoints require `Authorization: Bearer <JWT>` unless noted otherwise.

---
## 1. Create Quote

> BASIC

**Path:** `/api/booking/quote`  
**Method:** `POST`  
**Desc:** Generates a stateless booking quote and returns a signed `quote_token`.  

> REQUEST

**Headers**

| name | value | required | description |
| --- | --- | --- | --- |
| Authorization | Bearer `<jwt>` | YES | Access token from login |
| Content-Type | application/json | YES | — |

**Body**

| name | type | description |
| --- | --- | --- |
| product_type | string | `transport` \| `hotel` \| `attraction` |
| currency | string | Currency code (e.g. `JPY`) |
| party_size | integer | Travellers in the party |
| params | object | Product-specific parameters |

**Request Example (hotel)**

```json
{
  "product_type": "hotel",
  "currency": "JPY",
  "party_size": 2,
  "params": {
    "city": "Tokyo",
    "check_in": "2025-11-02",
    "check_out": "2025-11-05",
    "stars": 3,
    "room_type": "twin",
    "breakfast": false
  }
}
```

> RESPONSE

**Headers**

| name | value | required | description |
| --- | --- | --- | --- |
| content-type | application/json;charset=UTF-8 | NO | — |

**Body**

| name | type | description |
| --- | --- | --- |
| code | integer | 1 success; 0 failure |
| msg | string | Message string |
| data | object | Quote payload |
| └─ quote_token | string | Signed quote token |
| └─ expires_at | string | ISO-8601 expiry timestamp |
| └─ items | array | Quote line items |
| &emsp;└─ sku | string | Inventory code |
| &emsp;└─ unit_price | number | Unit price |
| &emsp;└─ quantity | integer | Quantity (e.g. passengers or nights) |
| &emsp;└─ fees | number | Additional fees |
| &emsp;└─ total | number | Total amount (unit price * qty + fees) |
| &emsp;└─ currency | string | Currency code |
| &emsp;└─ availability | integer | Simulated availability |
| &emsp;└─ meta | object | Supplemental metadata |
| &emsp;└─ cancellation_policy | string | Cancellation policy text |

**Response Example**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
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
          "room_type": "twin",
          "hotel_id": "HTL_42"
        },
        "cancellation_policy": "48h prior: full refund"
      }
    ]
  }
}
```

---
## 2. Prepare Itinerary Quote

> BASIC

**Path:** `/api/booking/itinerary/quote`  
**Method:** `POST`  
**Desc:** Generates a signed itinerary quote token that bundles multiple products (transport, hotel, attraction, etc.) into one payload. The AI planner calls this after producing a draft itinerary.  

> REQUEST

**Headers**

| name | value | required | description |
| --- | --- | --- | --- |
| Authorization | Bearer `<jwt>` | YES | Access token |
| Content-Type | application/json | YES | — |

**Body**

| name | type | description |
| --- | --- | --- |
| itinerary_id | string | Client-assigned identifier for the itinerary draft |
| currency | string | Currency shared by all items (e.g. `JPY`) |
| items | array | At least one itinerary item |
| └─ reference | string | Stable identifier for the item (used for subset confirm) |
| └─ product_type | string | `transport` \| `hotel` \| `attraction` |
| └─ party_size | integer | Travellers for this item |
| └─ params | object | Product-specific parameters |

**Request Example**

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

> RESPONSE

**Body**

| name | type | description |
| --- | --- | --- |
| code | integer | 1 success; 0 failure |
| msg | string | Message |
| data | object | Payload |
| └─ quote_token | string | Signed itinerary token |
| └─ expires_at | string | ISO-8601 expiry timestamp |
| └─ currency | string | Currency applied to all items |
| └─ bundle_total | number | Sum of totals for all items |
| └─ bundle_fees | number | Sum of fees for all items |
| └─ items | array | Per-item pricing |
| &emsp;└─ reference | string | Matches request reference |
| &emsp;└─ product_type | string | Product type |
| &emsp;└─ party_size | integer | Party size |
| &emsp;└─ total | number | Total for this item |
| &emsp;└─ fees | number | Fees for this item |
| &emsp;└─ quote_items | array | Detailed line items mirroring single-quote schema |

---
## 3. Confirm Booking

> BASIC

**Path:** `/api/booking/confirm`  
**Method:** `POST`  
**Desc:** Validates a quote token (single or itinerary), simulates payment, persists an order, and returns voucher + invoice details. Supports optional idempotency and itinerary subset confirmation.  

> REQUEST

**Headers**

| name | value | required | description |
| --- | --- | --- | --- |
| Authorization | Bearer `<jwt>` | YES | Access token |
| Content-Type | application/json | YES | — |
| Idempotency-Key | `<uuid>` | NO | Use to deduplicate retries |

**Body**

| name | type | description |
| --- | --- | --- |
| quote_token | string | Token returned from `/api/booking/quote` or `/api/booking/itinerary/quote` |
| payment_token | string | Mock payment token, e.g. `pm_mock_4242` |
| item_refs | array[string] | OPTIONAL. When confirming an itinerary quote, pass the subset of item references to confirm. Omit (or empty array) to confirm the whole bundle. Ignored for legacy single-product quotes. |

**Request Example**

```json
{
  "quote_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "payment_token": "pm_mock_4242"
}
```

> RESPONSE

**Headers**

| name | value | required | description |
| --- | --- | --- | --- |
| content-type | application/json;charset=UTF-8 | NO | — |

**Body**

| name | type | description |
| --- | --- | --- |
| code | integer | 1 success; 0 failure |
| msg | string | Message string |
| data | object | Confirmation payload |
| └─ status | string | `CONFIRMED` on success |
| └─ voucher_code | string | Voucher reference, e.g. `VCH-7GQ2-M9K1` |
| └─ invoice_id | string | Invoice reference, e.g. `INV_5566` |

**Response Example**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "status": "CONFIRMED",
    "voucher_code": "VCH-7GQ2-M9K1",
    "invoice_id": "INV_5566"
  }
}
```

---
### Error Codes

| HTTP | code | description |
| --- | --- | --- |
| 400 | ERR_VALIDATION | Request validation failed |
| 400 | ERR_PAYMENT_TOKEN | Invalid payment token |
| 402 | ERR_PAYMENT_FAILED | Payment authorization failed |
| 409 | ERR_QUOTE_EXPIRED | Quote expired or price drift detected |
| 409 | ERR_INVALID_QUOTE_TOKEN | Quote token invalid or signature mismatch |
| 409 | ERR_IDEMPOTENCY_MISMATCH | Idempotency key reused with different quote |
| 500 | ERR_INTERNAL | Unexpected server error |
