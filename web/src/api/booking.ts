import { apiRequest } from './client';

export type BookingProductType = 'transportation' | 'hotel' | 'attraction' | string;

export interface BookingQuotePayload {
    productType: BookingProductType;
    currency: string;
    partySize: number;
    params: Record<string, unknown>;
    tripId: number;
    entityId: number;
    itemReference: string;
}

export interface BookingQuoteSummary {
    voucherCode?: string | null;
    invoiceId?: string | null;
    status?: string | null;
    currency?: string | null;
    totalAmount?: number | null;
}

export interface BookingItem {
    entityId: number;
    tripId: number;
    productType: BookingProductType;
    title?: string | null;
    subtitle?: string | null;
    date?: string | null;
    time?: string | null;
    status: string;
    reservationRequired?: boolean | null;
    price?: number | null;
    currency?: string | null;
    imageUrl?: string | null;
    metadata?: Record<string, unknown> | null;
    quoteRequest: BookingQuotePayload;
    quoteSummary?: BookingQuoteSummary | null;
}

type QuoteApiPayload = {
    product_type: string;
    currency: string;
    party_size: number;
    params: Record<string, unknown>;
    trip_id: number;
    entity_id: number;
    item_reference: string;
};

type ItineraryQuoteItemPayload = {
    reference: string;
    product_type: string;
    party_size: number;
    params: Record<string, unknown>;
    entity_id: number;
};

type ItineraryQuotePayload = {
    itinerary_id: string;
    currency: string;
    items: ItineraryQuoteItemPayload[];
    trip_id: number;
};

function toQuoteRequestBody(payload: BookingQuotePayload): QuoteApiPayload {
    return {
        product_type: payload.productType,
        currency: payload.currency,
        party_size: payload.partySize,
        params: payload.params ?? {},
        trip_id: payload.tripId,
        entity_id: payload.entityId,
        item_reference: payload.itemReference,
    };
}

export async function getBookingItems(tripId: number | string) {
    return apiRequest<BookingItem[]>(`/api/booking?tripId=${encodeURIComponent(String(tripId))}`);
}

export async function confirmBooking(payload: BookingQuotePayload) {
    return apiRequest<void>('/api/booking/quote', {
        method: 'POST',
        body: toQuoteRequestBody(payload),
    });
}

export async function confirmAllBookings(tripId: number, items: BookingItem[]) {
    const pendingItems = items.filter((item) => {
        const status = item.status?.toLowerCase?.() ?? '';
        const isConfirmed = status === 'confirm' || status === 'confirmed';
        return (item.reservationRequired ?? true) && !isConfirmed;
    });
    if (pendingItems.length === 0) {
        return;
    }
    const currency =
        pendingItems.find((item) => item.quoteRequest?.currency)?.quoteRequest?.currency ||
        pendingItems.find((item) => item.currency)?.currency ||
        'AUD';

    const body: ItineraryQuotePayload = {
        itinerary_id: `iti_${tripId}`,
        currency: currency ?? 'AUD',
        items: pendingItems.map((item) => ({
            reference: item.quoteRequest.itemReference,
            product_type: item.quoteRequest.productType,
            party_size: item.quoteRequest.partySize,
            params: item.quoteRequest.params ?? {},
            entity_id: item.quoteRequest.entityId,
        })),
        trip_id: tripId,
    };

    return apiRequest<void>('/api/booking/itinerary/quote', {
        method: 'POST',
        body,
    });
}
