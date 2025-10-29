import { apiRequest } from './client';

export type MapTravelMode = 'driving' | 'walking' | 'bicycling' | 'transit';

export interface MapRouteRequest {
    origin: string;
    destination: string;
    travelMode?: MapTravelMode | string;
}

export interface MapRouteResponse {
    travelMode?: string | null;
    routeSummary?: string | null;
    distanceText?: string | null;
    distanceMeters?: number | null;
    durationText?: string | null;
    durationSeconds?: number | null;
    overviewPolyline?: string | null;
    embedUrl?: string | null;
    shareUrl?: string | null;
    warnings?: string[] | null;
}

export async function generateRoute(payload: MapRouteRequest) {
    return apiRequest<MapRouteResponse>('/api/map/route', {
        method: 'POST',
        body: payload,
    });
}
