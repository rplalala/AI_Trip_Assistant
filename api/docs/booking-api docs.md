# Booking API Docs

**API Version:** v1.0  

All booking endpoints require `Authorization: Bearer <JWT>` unless noted otherwise.

---

## Integration Notes

### Backend flow (server → booking API)
- Incoming requests hit `BookingController`, which delegates to `BookingFacade` (and other service-entry points such as `BookingServiceImpl`) for validation and enrichment.
- `BookingServiceImpl` isn’t wired into any REST controller today—BookingController uses `BookingFacade`.  It backs the integration tests in BookingServiceIntegrationTest.java, which exercise the full quote workflow (price enrichment, persistence, status updates). Dropping the service would mean rewriting or losing those tests.
If anything else in the code wants to orchestrate booking without going through the controller façade (batch jobs, scheduled retries, future features), the service provides that reusable entry point.
- All server-side booking workflows call the shared Feign `BookingClient`, which issues HTTP POST requests to the external booking service (`external-service` module).
- Before calling the external API, the facade loads pricing and metadata from JPA repositories (`TripHotelRepository`, `TripTransportationRepository`, `TripAttractionRepository`) using the supplied `trip_id` and `entity_id`, then injects the data into the outbound payload.
- Successful responses are persisted through `TripBookingQuoteRepository`, and the associated trip entities have their `status` updated to `confirm`.

### Frontend flow (web → server)
- The web client invokes `/api/booking/quote` or `/api/booking/itinerary/quote` with the identifiers returned from itinerary generation.
- The frontend must pass `trip_id`, `entity_id`, and `item_reference` so the backend can hydrate the payload from the database and map results back to itinerary cards.
- The UI consumes the returned voucher, invoice, and pricing information and refreshes itinerary items to reflect the confirmed status.

### Database interaction
- Pricing and metadata are sourced from `trip_transportation`, `trip_hotel`, and `trip_attraction` tables via the repositories noted above.
- Quote history is stored in `trip_booking_quote`, linking booking vouchers/invoices back to the underlying trip entities.
- Status transitions (for example, to `confirm`) are written back to the original trip tables so itinerary views display the confirmed state.

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
| trip_id | number | Trip identifier used for persistence and status updates; required in current workflow when quoting stored itinerary items |
| entity_id | number | Trip entity identifier (e.g. hotel ID) used to source stored pricing; required when `trip_id` is supplied |
| item_reference | string | Stable reference used to link itinerary pricing and booking quotes; required when persisting quotes for existing trip items |

**Request Example (hotel)**

```json
{
  "product_type": "hotel",
  "currency": "AUD",
  "party_size": 3,
  "params": {
    "city": "Beijin",
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
        "voucher_code": "VCH-0137-0E0E",
        "invoice_id": "INV_8487",
        "items": [
            {
                "sku": "HTL_BEIJING_CENTRAL_HOTEL_TRIPLE_ROOM_2025-10-30",
                "unit_price": 800,
                "quantity": 1,
                "fees": 0,
                "total": 800,
                "currency": "AUD",
                "meta": {
                    "nights": 4,
                    "date": "2025-10-30",
                    "room_type": "Triple room",
                    "time": "15:00",
                    "title": "Check into Beijing Hotel",
                    "reservation_required": true,
                    "hotel_name": "Beijing Central Hotel",
                    "people": 3,
                    "status": "confirm"
                },
                "cancellation_policy": "48h prior: full refund"
            }
        ]
    }
}
```

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
| └─ entity_id | number | Trip entity identifier associated with the reference; required so the service can load pricing/status from the itinerary database |
| trip_id | number | Trip identifier applied to all items; required to persist pricing and update itinerary item statuses |

**Request Example**

```json
{
  "itinerary_id": "iti_tokyo",
  "currency": "JPY",
  "trip_id": 1,
  "items": [
    {
    "reference": "transport_narita_transfer",
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
        "price": 54000,
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
        "provider": "Quantas",
        "ticket_type": "economy",
        "people": 2,
        "price": 1000
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

**Response Example**

```json
{
    "code": 1,
    "msg": "success",
    "data": {
        "voucher_code": "VCH-C7D2-9A3E",
        "invoice_id": "INV_2847",
        "currency": "JPY",
        "items": [
            {
                "reference": "transport_narita_transfer",
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
                        "sku": "TP_TOKYO,_JAPAN_SYDNEY,_AUSTRALIA_2025-11-04",
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
                            "to": "SYDNEY, AUSTRALIA",
                            "from": "TOKYO, JAPAN",
                            "reservation_required": true,
                            "ticket_type": "economy",
                            "time": "12:00",
                            "people": 2
                        },
                        "cancellation_policy": "No charge until 7 days prior; 25% after."
                    }
                ]
            }
        ],
        "bundle_total": 2200,
        "bundle_fees": 0
    }
}
```