export interface EventCategory {
  name: string;
  icon: string;
}

export interface FeaturedEvent {
  title: string;
  category: string;
  date: string;
  location: string;
  price?: string;
  image: string;
  alt: string;
  buttonText?: string;
}

export interface PromoDeal {
  eyebrow: string;
  title: string;
  buttonText: string;
  image: string;
  alt: string;
  isTransparentButton?: boolean;
}

export const EVENT_CATEGORIES: EventCategory[] = [
  { name: 'Concerts', icon: '🎵' },
  { name: 'Sports', icon: '⚽' },
  { name: 'Arts & Theater', icon: '🎭' },
  { name: 'Family', icon: '👪' },
  { name: 'Comedy', icon: '🗣️' },
  { name: 'More', icon: '🏢' },
];

export const PROMO_DEALS: PromoDeal[] = [
  {
    eyebrow: 'LIVE NATION PRESENTS',
    title: 'Summer of Live Lawn 4-Packs',
    buttonText: 'See Deals',
    image: 'https://images.unsplash.com/photo-1508098682722-e99c43a406b2?auto=format&fit=crop&w=700&q=80',
    alt: 'Basketball court in a stadium'
  },
  {
    eyebrow: 'CONFERENCE',
    title: 'Future Systems Summit',
    buttonText: 'Learn More',
    image: 'https://images.unsplash.com/photo-1540575467063-178a50c2df87?auto=format&fit=crop&w=700&q=80',
    alt: 'Auditorium stage with abstract background graphic',
    isTransparentButton: true
  }
];

export const FEATURED_EVENTS: FeaturedEvent[] = [
  {
    title: 'Electric Horizon Festival',
    category: 'Music',
    date: 'NOV 24 • SUN • 8:00 PM',
    location: 'Grand Arena • City Center',
    image: 'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=700&q=80',
    alt: 'Crowd at a concert with lights',
    buttonText: 'See Tickets'
  },
  {
    title: 'Global Championship Finals',
    category: 'Sports',
    date: 'DEC 12 • THU • 7:30 PM',
    location: 'National Stadium • Sector 4',
    image: 'https://images.unsplash.com/photo-1519766304817-4f37bda74a27?auto=format&fit=crop&w=700&q=80',
    alt: 'Basketball match in progress',
    buttonText: 'See Tickets'
  },
  {
    title: 'Future Systems Summit',
    category: 'Conference',
    date: 'JAN 18 • WED • 9:00 AM',
    location: 'Tech Hub Center • Level 2',
    image: 'https://images.unsplash.com/photo-1515187029135-18ee286d815b?auto=format&fit=crop&w=700&q=80',
    alt: 'Presenter speaking on tech conference stage',
    buttonText: 'See Tickets'
  },
  {
    title: 'Season Passes 2026',
    category: 'Passes',
    date: 'NOW AVAILABLE NOW',
    location: 'Multiple Locations • All Access',
    image: '', // Will use custom blue container in html
    alt: 'Blue ticket pass background',
    buttonText: 'See Passes'
  },
];

