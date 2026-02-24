import request from "../../utils/request";

/**
 * 搜索房源
 */
export function searchHomestays(searchRequest: {
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
  checkInDate?: string;
  checkOutDate?: string;
  requiredAmenities?: string[];
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: string;
}) {
  console.log("搜索房源，参数:", searchRequest);
  return request.post("/api/homestays/search", searchRequest);
}
