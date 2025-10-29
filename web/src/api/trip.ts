import { apiRequest } from './client';

export interface generateTripPayload {
    fromCountry: string;
    fromCity: string;
    toCountry: string;
    toCity: string;
    currency: string;
    preferences: string;
    budget: number;
    people: number;
    startDate: string;
    endDate: string;

}

export interface TripInsightsResponse {
    id: number;
    title: string;
    content: string;
    theme: string;
    icon: string;
}
export interface TripDetail {
    tripId: number;
    fromCountry: string;
    fromCity: string;
    toCountry: string;
    toCity: string;
    budget: number;
    people: number;
    startDate: string;
    endDate: string;
    imgUrl?: string | null;
}

export interface TimeLineDTO {
    date: string;
    imageUrl?: string | null;
    summary?: string | null;
    maxTemperature?: number | null;
    minTemperature?: number | null;
    weatherCondition?: string | null;
    attraction?: AttractionTimeLineDTO[] | null;
    hotel?: HotelTimeLineDTO[] | null;
    transportation?: TransportationTimeLineDTO[] | null;
}

export interface AttractionTimeLineDTO {
    location?: string | null;
    time?: string | null;
    title?: string | null;
}

export interface HotelTimeLineDTO {
    hotelName?: string | null;
    time?: string | null;
    title?: string | null;
}

export interface TransportationTimeLineDTO {
    time?: string | null;
    title?: string | null;
}

export interface ModifyPlanPayload {
    secondPreference: string;
}

export async function generateTrip(payload: generateTripPayload) {
    return apiRequest<void>('/api/trip/generate-plan', {
        method: 'POST',
        body: payload,
    });
}

export async function regenerateTrip(tripId: string | number, payload: ModifyPlanPayload) {
    const qs = new URLSearchParams({ tripId: String(tripId) }).toString();
    return apiRequest<void>(`/api/trip/regenerate-plan?${qs}`, {
        method: 'POST',
        body: payload,
    });
}

export async function getTripInsights(tripId: string) {
    return apiRequest<TripInsightsResponse[]>(`/api/trip/insights?tripId=${tripId}`);
}

export async function getTripDetails() {
    return apiRequest<TripDetail[]>('/api/trip/details');
}

export async function deleteTrips(tripIds: number[]) {
    const params = new URLSearchParams();
    for (const id of tripIds) params.append('tripIds', String(id));
    const qs = params.toString();
    return apiRequest<void>(`/api/trip${qs ? `?${qs}` : ''}` , { method: 'DELETE' });
}

export async function getTripTimeline(tripId: string) {
    return apiRequest<TimeLineDTO[]>(`/api/trip/timeline?tripId=${encodeURIComponent(tripId)}`);
}
