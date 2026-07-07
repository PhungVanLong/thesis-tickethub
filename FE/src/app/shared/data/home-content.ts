export interface EventCategory {
  name: string;
  icon: string;
}

export interface FeaturedEvent {
  title: string;
  category: string;
  date: string;
  location: string;
  price: string;
  image: string;
  alt: string;
}

export const EVENT_CATEGORIES: EventCategory[] = [
  { name: 'Âm nhạc', icon: '♪' },
  { name: 'Sân khấu', icon: '◆' },
  { name: 'Workshop', icon: '✦' },
  { name: 'Thể thao', icon: '●' },
  { name: 'Lễ hội', icon: '✹' },
  { name: 'Gia đình', icon: '◎' },
];

export const FEATURED_EVENTS: FeaturedEvent[] = [
  {
    title: 'Rooftop Acoustic: Thành phố lên đèn',
    category: 'Âm nhạc',
    date: '22.06',
    location: 'Chill Skybar, Quận 1',
    price: '320.000đ',
    image:
      'https://images.unsplash.com/photo-1516280440614-37939bbacd81?auto=format&fit=crop&w=700&q=80',
    alt: 'Ca sĩ biểu diễn dưới ánh đèn sân khấu',
  },
  {
    title: 'Múa đương đại: Những đường cong im lặng',
    category: 'Sân khấu',
    date: '25.06',
    location: 'Nhà hát Thành phố',
    price: '590.000đ',
    image:
      'https://images.unsplash.com/photo-1503095396549-807759245b35?auto=format&fit=crop&w=700&q=80',
    alt: 'Sân khấu biểu diễn nghệ thuật với ánh đèn tím',
  },
  {
    title: 'Design Systems for Product Teams',
    category: 'Workshop',
    date: '29.06',
    location: 'Dreamplex Nguyễn Trung Ngạn',
    price: '250.000đ',
    image:
      'https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?auto=format&fit=crop&w=700&q=80',
    alt: 'Nhóm người tham gia workshop sáng tạo',
  },
  {
    title: 'Summer Run Festival 2026',
    category: 'Thể thao',
    date: '05.07',
    location: 'Công viên Sala',
    price: '180.000đ',
    image:
      'https://images.unsplash.com/photo-1502224562085-639556652f33?auto=format&fit=crop&w=700&q=80',
    alt: 'Vận động viên chạy bộ trên đường đua',
  },
];
