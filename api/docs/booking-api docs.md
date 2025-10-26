# Booking API Docs

**API Version:** v1.0  

All booking endpoints require `Authorization: Bearer <JWT>` unless noted otherwise.

---
## 1. Create Quote

> BASIC

**Path:** `/api/booking/quote`  
**Method:** `POST`  
**Desc:** Generates a booking quote and immediately returns voucher and invoice references with line items.  

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
| trip_id | number | *(optional)* Trip identifier used for persistence; when present with `entity_id`, pricing is loaded from the itinerary record instead of simulated values |
| entity_id | number | *(optional)* Trip entity identifier (e.g. hotel ID) used to source stored pricing |
| item_reference | string | *(optional)* Stable reference used to link itinerary pricing |

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
  },
  "trip_id": 4821,
  "entity_id": 73,
  "item_reference": "hotel_73"
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
| └─ voucher_code | string | Voucher reference created by the booking service |
| └─ invoice_id | string | Invoice reference created by the booking service |
| └─ items | array | Quote line items |
| &emsp;└─ sku | string | Inventory code |
| &emsp;└─ unit_price | number | Unit price |
| &emsp;└─ quantity | integer | Quantity (e.g. passengers or nights) |
| &emsp;└─ fees | number | Additional fees |
| &emsp;└─ total | number | Total amount (unit price * qty + fees) |
| &emsp;└─ currency | string | Currency code |
| &emsp;└─ meta | object | Supplemental metadata |
| &emsp;└─ cancellation_policy | string | Cancellation policy text |

**Response Example**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "voucher_code": "VCH-DF21-BA34",
    "invoice_id": "INV_4330",
    "items": [
      {
        "sku": "HTL_BEIJING_CENTRAL_HOTEL_TRIPLE_ROOM_2025-10-30",
        "unit_price": 800,
        "quantity": 1,
        "fees": 0,
        "total": 800,
        "currency": "AUD",
        "meta": {
          "hotel_name": "Beijing Central Hotel",
          "reservation_required": true,
          "title": "Check into Beijing Hotel",
          "time": "15:00",
          "room_type": "Triple room",
          "date": "2025-10-30",
          "nights": 4,
          "status": "confirmed",
          "people": 3
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
**Desc:** Generates a bundled itinerary quote with voucher/invoice references for all items. The AI planner calls this after producing a draft itinerary.  

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
| └─ reference | string | Stable identifier for the item (used to match itinerary entries) |
| └─ product_type | string | `transport` \| `hotel` \| `attraction` |
| └─ party_size | integer | Travellers for this item |
| └─ params | object | Product-specific parameters |
| └─ entity_id | number | *(optional)* Trip entity identifier associated with the reference; enables price lookup from the itinerary database |
| trip_id | number | *(optional)* Trip identifier applied to all items; required when the service should load pricing for each entity |

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
      },
      "entity_id": 73
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
      },
      "entity_id": 91
    }
  ],
  "trip_id": 4821
}
```

> RESPONSE

**Body**

| name | type | description |
| --- | --- | --- |
| code | integer | 1 success; 0 failure |
| msg | string | Message |
| data | object | Payload |
| └─ voucher_code | string | Voucher reference created for the itinerary |
| └─ invoice_id | string | Invoice reference created for the itinerary |
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

### Error Codes

| HTTP | code | description |
| --- | --- | --- |
| 400 | ERR_VALIDATION | Request validation failed |
| 500 | ERR_INTERNAL | Unexpected server error |
