# Booking Service API

This service exposes quote endpoints that power the AI Trip Assistant booking flow.

## 1. POST `/api/booking/quote`

Generate a booking quote. The response immediately returns voucher + invoice references alongside the priced line items so downstream systems can treat the quote as confirmed.

### Request Body

```json
{
  "product_type": "transport",
  "currency": "AUD",
  "party_size": 2,
  "params": {
    "from": "Sydney,Australia",
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

| Field | Type | Notes |
| --- | --- | --- |
| `product_type` | enum (`transport`,`hotel`,`attraction`) | Determines pricing strategy |
| `currency` | string | ISO currency code (JPY in MVP) |
| `party_size` | integer | Number of travellers |
| `params` | object | Product-specific parameters|
| `trip_id` | number | Upstream trip identifier used to persist quotes and update itinerary status |
| `entity_id` | number | Upstream entity identifier required for persistence + pricing lookup |

`entity_id`, `product_type`, and `trip_id` are madatory for finding correct item in corresponding tables (trip_hotel, trip_transportation, trip_attraction)

### Success Response

```json
{
    "code": 1,
    "msg": "success",
    "data": {
        "voucher_code": "VCH-7428-086D",
        "invoice_id": "INV_2667",
        "items": [
            {
                "sku": "TP_SYDNEY,_AUSTRALIA_TOKYO,_JAPAN_2025-10-26",
                "unit_price": 1000,
                "quantity": 1,
                "fees": 0,
                "total": 1000,
                "currency": "AUD",
                "meta": {
                    "date": "2025-10-26",
                    "provider": "Qantas",
                    "mode": "transport",
                    "status": "confirm",
                    "to": "TOKYO, JAPAN",
                    "from": "SYDNEY, AUSTRALIA",
                    "reservation_required": true,
                    "ticket_type": "economy",
                    "time": "10:00",
                    "people": 2
                },
                "cancellation_policy": "No charge until 7 days prior; 25% after."
            }
        ]
    }
}
```

## 2. POST `/api/booking/itinerary/quote`

Bundle multiple products into a single itinerary quote. The response lists each itinerary item, returns bundle totals, and issues voucher + invoice codes for the group. Item `reference` values remain so the UI can map priced results back to itinerary entries.

### Request Body

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
        "provider": "Quantas",
        "ticket_type": "economy",
        "people": 2,
        "price": 1000
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
| └─ `reference` | string | Stable handle for the item (used to map bundle responses) |
| └─ `product_type` | enum | Same as single quote |
| └─ `party_size` | integer | Travellers for this item |
| └─ `params` | object | Same schema as single quote for the given product type |
| └─ `entity_id` | number | Upstream entity identifier required so the service can resolve stored pricing |
| `trip_id` | number | Upstream trip identifier that must accompany each request so pricing and statuses persist correctly |

`entity_id`, `product_type`, and `trip_id` are madatory for finding correct item in corresponding tables (trip_hotel, trip_transportation, trip_attraction)

### Success Response

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
## Pricing Notes
- Deterministic pricing per product type using request parameters as the seed – re-quoting with the same inputs gives the same price.
- Generated voucher + invoice identifiers are mock values suitable for demos.

## Integration Notes

### Server workflow
- The Spring Boot API (`api` module) calls this service exclusively through the shared Feign `BookingClient` used by `BookingFacade` and `BookingServiceImpl`.
- Before invoking the external endpoint, the service layer enriches the request by loading itinerary data from the trip repositories (`TripHotelRepository`, `TripTransportationRepository`, `TripAttractionRepository`).
- After receiving a quote, the server persists it via `TripBookingQuoteRepository` and updates the corresponding trip item status to `confirm`.

### Frontend workflow
- The frontend never calls the external service directly; it submits requests to `/api/booking/quote` or `/api/booking/itinerary/quote` on the API server.
- Those requests must include `trip_id`, `entity_id`, `product_type`, and `item_reference` so the server can reconcile quotes with itinerary entries and display confirmed pricing in the UI.

### Database usage
- Pricing inputs are sourced from `trip_transportation`, `trip_hotel`, and `trip_attraction` using the IDs supplied in the request.
- Confirmed quote results are stored in `trip_booking_quote`, providing voucher/invoice tracking and linking back to the original itinerary items.
