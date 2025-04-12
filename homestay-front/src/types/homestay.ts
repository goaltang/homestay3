export interface Homestay {
  id?: number;
  title: string;
  type: string;
  price: string;
  status: string;
  maxGuests: number;
  minNights: number;
  province: string;
  city: string;
  district: string;
  address: string;
  amenities: string[];
  description: string;
  coverImage: string;
  images: string[];
  ownerUsername?: string;
  ownerName?: string;
  featured: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface HomestaySearchRequest {
  keyword?: string;
  location?: string;
  propertyType?: string;
  minGuests?: number;
  maxGuests?: number;
  minPrice?: number;
  maxPrice?: number;
  checkInDate?: string;
  checkOutDate?: string;
  hasWifi?: boolean;
  hasAirConditioning?: boolean;
  hasKitchen?: boolean;
  hasWasher?: boolean;
  hasParking?: boolean;
  hasPool?: boolean;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: string;
}
