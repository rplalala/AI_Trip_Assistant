import { apiRequest } from './client';

export type MapTravelMode = 'driving' | 'walking' | 'bicycling' | 'transit';
export type MapProvider = 'google' | 'amap';

export interface MapRouteRequest {
    origin: string;
    destination: string;
    travelMode?: MapTravelMode | string;
    provider?: MapProvider; // explicit provider override to send to backend
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
    provider?: MapProvider | null; // backend returns which provider was used
}

export async function generateRoute(payload: MapRouteRequest) {
    return apiRequest<MapRouteResponse>('/api/map/route', {
        method: 'POST',
        body: payload,
    });
}
