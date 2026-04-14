import request from "../../utils/request";

export interface HomestaySearchRequest {
  keyword?: string;
  location?: string;
  provinceCode?: string;
  cityCode?: string;
  districtCode?: string;
  propertyType?: string;
  minGuests?: number;
  maxGuests?: number;
  minPrice?: number;
  maxPrice?: number;
  northEastLat?: number;
  northEastLng?: number;
  southWestLat?: number;
  southWestLng?: number;
  zoom?: number;
  latitude?: number;
  longitude?: number;
  radiusKm?: number;
  limit?: number;
  checkInDate?: string;
  checkOutDate?: string;
  requiredAmenities?: string[];
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: string;
}

export interface MapCluster {
  latitude: number;
  longitude: number;
  count: number;
}

/**
 * 搜索房源
 */
export function searchHomestays(searchRequest: HomestaySearchRequest) {
  console.log("搜索房源，参数:", searchRequest);
  return request.post("/api/homestays/search", searchRequest);
}

export function mapSearchHomestays(searchRequest: HomestaySearchRequest) {
  console.log("Map search homestays, params:", searchRequest);
  return request.post("/api/homestays/map-search", searchRequest);
}

export function getMapClusters(searchRequest: HomestaySearchRequest) {
  console.log("Get map clusters, params:", searchRequest);
  return request.post<MapCluster[]>("/api/homestays/map-clusters", searchRequest);
}

export function getNearbyHomestays(searchRequest: HomestaySearchRequest) {
  console.log("Get nearby homestays, params:", searchRequest);
  return request.post("/api/homestays/nearby", searchRequest);
}

export function landmarkSearchHomestays(searchRequest: HomestaySearchRequest) {
  console.log("Landmark nearby homestay search, params:", searchRequest);
  return request.post("/api/homestays/landmark-search", searchRequest);
}
