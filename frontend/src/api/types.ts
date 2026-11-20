export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface User {
  id: string;
  username: string;
  email: string;
  city: string | null;
  shippingAddress: string | null;
  hasPaymentMethod: boolean;
  bio: string | null;
  avatarUrl: string | null;
}

export interface Session {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface Artwork {
  id: string;
  artistId: string;
  title: string;
  description: string | null;
  technique: string | null;
  dimensions: string | null;
  weightKg: number | null;
  suggestedPrice: number | null;
  tags: string[];
  coverUrl: string | null;
  createdAt: string;
}

export interface Media {
  id: string;
  kind: 'IMAGE' | 'VIDEO';
  url: string;
  contentType: string;
  cover: boolean;
}

export interface ArtworkDetail {
  artwork: Artwork;
  media: Media[];
  favoritesCount: number;
  favorited: boolean;
}

export type AuctionCurrency = 'USD' | 'COP' | 'EUR';

export interface Auction {
  id: string;
  artworkId: string;
  artworkTitle: string;
  coverUrl: string | null;
  type: 'SIMPLE' | 'IMMEDIATE' | 'DUTCH' | 'SEALED';
  currency: AuctionCurrency | null;
  floorPrice: number | null;
  status: 'ACTIVE' | 'CLOSED' | 'CANCELLED';
  basePrice: number;
  currentPrice: number;
  immediatePurchasePrice: number | null;
  bidCount: number;
  startDate: string;
  endDate: string;
}

export interface Bid {
  id: string;
  buyerId: string;
  amount: number;
  bidTime: string;
}

export interface AuctionDetail {
  auction: Auction;
  highestBid: Bid | null;
  minimumNextBid: number | null;
}

export interface Order {
  checkout: boolean;
  id: string;
  auctionId: string;
  winnerId: string;
  subtotal: number;
  shippingCost: number;
  platformFee: number;
  totalPaid: number;
  paymentStatus: 'PENDING' | 'PROCESSING' | 'PAID' | 'FAILED' | 'EXPIRED';
  shippingStatus: 'PENDING' | 'SHIPPED' | 'DELIVERED';
  carrier: string | null;
  trackingNumber: string | null;
  shippedAt: string | null;
  deliveredAt: string | null;
  createdAt: string;
  paymentDueAt: string;
  snapshot: {
    title: string;
    currency: string;
    recipient: string | null;
    shippingAddress: string | null;
    city: string | null;
  };
}

export interface Notification {
  id: string;
  title: string;
  message: string;
  data: Record<string, string>;
  read: boolean;
  createdAt: string;
}
