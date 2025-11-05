/**
 * TrackAsia Places API Client
 */

const BASE_URL = 'https://maps.track-asia.com/api/v2/place';

export interface TrackAsiaPlaceGeometry {
  location: { lat: number; lng: number };
}

export interface TrackAsiaPlaceResult {
  place_id: string;
  name: string;
  formatted_address?: string;
  adr_address?: string;
  geometry: TrackAsiaPlaceGeometry;
  types?: string[];
}

export interface TrackAsiaSearchResponse {
  status: string;
  results: TrackAsiaPlaceResult[];
  error_message?: string;
}

function getApiKey(): string {
  const key = process.env.TRACKASIA_API_KEY || process.env.TRACK_ASIA_API_KEY || '';
  if (!key) {
    // Do not throw here; callers can handle empty key scenarios gracefully
    // to allow local-only behavior when key is missing.
  }
  return key;
}

export async function nearbySearch(params: {
  lat: number;
  lon: number;
  radius: number;
  types?: string[];
  newAdmin?: boolean;
  includeOldAdmin?: boolean;
}): Promise<TrackAsiaSearchResponse> {
  const key = getApiKey();
  const url = new URL(`${BASE_URL}/nearbysearch/json`);
  url.searchParams.set('location', `${params.lat},${params.lon}`);
  url.searchParams.set('radius', String(Math.max(1, Math.min(50000, Math.round(params.radius)))));
  if (params.types && params.types.length > 0) {
    url.searchParams.set('type', params.types.join('|'));
  }
  if (params.newAdmin !== undefined) {
    url.searchParams.set('new_admin', String(params.newAdmin));
  }
  if (params.includeOldAdmin) {
    url.searchParams.set('include_old_admin', 'true');
  }
  if (key) url.searchParams.set('key', key);

  const response = await fetch(url.toString(), { method: 'GET' });
  const data = (await response.json()) as TrackAsiaSearchResponse;
  return data;
}

export async function textSearch(params: {
  query: string;
  newAdmin?: boolean;
  includeOldAdmin?: boolean;
}): Promise<TrackAsiaSearchResponse> {
  const key = getApiKey();
  const url = new URL(`${BASE_URL}/textsearch/json`);
  url.searchParams.set('query', params.query);
  if (params.newAdmin !== undefined) {
    url.searchParams.set('new_admin', String(params.newAdmin));
  }
  if (params.includeOldAdmin) {
    url.searchParams.set('include_old_admin', 'true');
  }
  if (key) url.searchParams.set('key', key);

  const response = await fetch(url.toString(), { method: 'GET' });
  const data = (await response.json()) as TrackAsiaSearchResponse;
  return data;
}
