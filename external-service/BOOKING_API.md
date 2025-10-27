# Booking Service API

This service exposes quote endpoints that power the AI Trip Assistant booking flow.

## 1. POST `/api/booking/quote`

Generate a booking quote. The response immediately returns voucher + invoice references alongside the priced line items so downstream systems can treat the quote as confirmed.

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
  },
  "trip_id": 4821,
  "entity_id": 91,
  "item_reference": "transport_91"
}
```

| Field | Type | Notes |
| --- | --- | --- |
| `product_type` | enum (`transport`,`hotel`,`attraction`) | Determines pricing strategy |
| `currency` | string | ISO currency code (JPY in MVP) |
| `party_size` | integer | Number of travellers |
| `params` | object | Product-specific parameters (examples below) |
| `trip_id` | number | Upstream trip identifier used to persist quotes and update itinerary status |
| `entity_id` | number | Upstream entity identifier required for persistence + pricing lookup |
| `item_reference` | string | External reference for downstream reconciliation; required when quoting stored itinerary items |

#### Param Examples
- **Transport**: `{"mode":"train|flight","from":"NRT","to":"Shinjuku","date":"2025-11-02","class":"economy","price":18000}`  
- **Hotel**: `{"city":"Tokyo","check_in":"2025-11-02","check_out":"2025-11-05","stars":3,"room_type":"twin","breakfast":false,"price":37800}`  
- **Attraction**: `{"city":"Tokyo","name":"teamLab Planets","date":"2025-11-03","session":"10:00","ticket_price":7500,"people":2}`  

### Success Response

```json
{
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
```

## 2. POST `/api/booking/itinerary/quote`

Bundle multiple products into a single itinerary quote. The response lists each itinerary item, returns bundle totals, and issues voucher + invoice codes for the group. Item `reference` values remain so the UI can map priced results back to itinerary entries.

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

### Success Response

```json
{
  "voucher_code": "VCH-BUNDLE-5521",
  "invoice_id": "INV_7721",
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
## Pricing Notes
- Deterministic pricing per product type using request parameters as the seed – re-quoting with the same inputs gives the same price.
- Generated voucher + invoice identifiers are mock values suitable for demos.
