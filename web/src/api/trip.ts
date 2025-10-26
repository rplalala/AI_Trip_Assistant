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

export async function generateTrip(payload: generateTripPayload) {
    return apiRequest<void>('/api/trip/generate-plan', {
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
