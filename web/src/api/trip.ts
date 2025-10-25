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

export async function generateTrip(payload: generateTripPayload) {
    return apiRequest<string>('/api/trip/test-generate', {
        method: 'POST',
        body: payload,
    });
}

export async function getTripInsights(tripId: string) {
    return apiRequest<TripInsightsResponse[]>(`/api/trip/insights?tripId=${tripId}`);
}
