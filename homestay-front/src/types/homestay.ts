// 设施项（与后端 AmenityDTO 对齐）
export interface AmenityItem {
  value?: string;
  label?: string;
  description?: string;
  icon?: string;
  categoryCode?: string;
  categoryName?: string;
  categoryIcon?: string;
  active?: boolean;
  usageCount?: number;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
}

export interface Homestay {
  id?: number;
  title: string;
  type: string;
  propertyTypeName?: string;
  price: string;
  status: string;
  maxGuests: number;
  minNights: number;
  maxNights?: number;
  provinceText?: string;
  cityText?: string;
  districtText?: string;
  addressDetail?: string;
  provinceCode?: string;
  cityCode?: string;
  districtCode?: string;
  amenities: AmenityItem[];
  description: string;
  coverImage: string;
  images: string[];
  ownerId?: number;
  ownerUsername?: string;
  ownerName?: string;
  ownerAvatar?: string;
  ownerRating?: number;
  distanceKm?: number;
  featured: boolean;
  autoConfirm?: boolean;
  checkInTime?: string;
  checkOutTime?: string;
  cancelPolicyType?: number;
  houseRules?: string;
  createdAt?: string;
  updatedAt?: string;
  groupId?: number;
  groupName?: string;
  groupCode?: string;
  groupColor?: string;
}

// 扩展详情页专用的Homestay接口
export interface HomestayDetail extends Homestay {
  rating?: number | null;
  reviewCount?: number;
  ownerId?: number;
  ownerAvatar?: string;
  propertyType?: string;
  bedrooms?: number;
  beds?: number;
  bathrooms?: number;
  distanceFromCenter?: number;
  provinceCode?: string;
  cityCode?: string;
  districtCode?: string;
  addressDetail?: string;
  suggestedFeatures?: SuggestedFeatureDTO[];
}

// 推荐特色功能
export interface SuggestedFeatureDTO {
  featureId: string;
  iconName: string;
  title: string;
  description: string;
  priority?: number;
}

// 评价相关
export interface Review {
  id: number;
  userName: string;
  userAvatar?: string;
  rating: number;
  content: string;
  createTime: string;
  response?: string;
  responseTime?: string;
  images?: string[];
}

export interface ReviewStatItem {
  name: string;
  score: number;
}

// 预订相关
export interface BookingDates {
  checkIn: Date | null;
  checkOut: Date | null;
  guests: number;
}

export interface CreateOrderPayload {
  homestayId: number;
  checkInDate: string;
  checkOutDate: string;
  guestCount: number;
  totalPrice: number;
  guestName: string;
  guestPhone: string;
}

// 地图相关
export interface MapData {
  lat: number;
  lng: number;
  staticMapUrl: string;
  isLoading: boolean;
  hasLocation: boolean;
}

export interface NearbyPlace {
  name: string;
  type: string;
  distance: number;
  address: string;
}

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
  checkInDate?: string;
  checkOutDate?: string;
  requiredAmenities?: string[];
  hasWifi?: boolean;
  hasAirConditioning?: boolean;
  hasKitchen?: boolean;
  hasWasher?: boolean;
  hasParking?: boolean;
  hasPool?: boolean;
  groupId?: number;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: string;
}

// API 相关类型
// 房源类型实体（来自后端 homestay_types 表）
export interface PropertyTypeInfo {
  id: number;
  code: string;
  name: string;
  icon?: string;
  description?: string;
  active?: boolean;
  sortOrder?: number;
  categoryId?: number;
  categoryName?: string;
}

// 兼容旧名称
export type HomestayType = PropertyTypeInfo;

export interface HomestayListParams {
  page?: number;
  size?: number;
  status?: string;
  type?: string;
}

// 设施相关类型
export interface AmenityOption {
  id?: number;
  value: string;
  label: string;
  icon?: string;
}

export interface AmenityCategoryOption {
  code?: string;
  name: string;
  icon?: string;
  sortOrder?: number;
  amenities: AmenityOption[];
}

// 审核相关类型
export interface AuditRecord {
  id: number;
  homestayId: number;
  homestayTitle?: string;
  reviewerId: number;
  reviewerName: string;
  oldStatus: string;
  newStatus: string;
  actionType: string;
  reviewReason: string;
  reviewNotes?: string;
  createdAt: string;
  ipAddress?: string;
}

export interface AuditHistoryResponse {
  content: AuditRecord[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface OwnerAuditStats {
  total: number;
  draft: number;
  pending: number;
  active: number;
  inactive: number;
  rejected: number;
  suspended: number;
  avgReviewTime?: number;
}

// 房源分组相关
export interface HomestayGroup {
  id: number;
  name: string;
  code?: string;
  description?: string;
  icon?: string;
  color?: string;
  ownerId: number;
  ownerUsername?: string;
  sortOrder: number;
  isDefault: boolean;
  enabled: boolean;
  homestayCount: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface HomestayGroupRequest {
  name: string;
  code?: string;
  description?: string;
  icon?: string;
  color?: string;
  sortOrder?: number;
  enabled?: boolean;
}
