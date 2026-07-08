import { Language } from '../../core/services/language.service';

export const TRANSLATIONS: Record<Language, Record<string, string>> = {
  Vie: {
    // Navigation Header
    'nav.organizer': 'Đăng ký làm tổ chức',
    'nav.myTickets': 'Vé của tôi',
    'nav.account': 'Tài khoản',
    'nav.signIn': 'Đăng nhập / Đăng ký',
    'nav.concerts': 'Âm nhạc',
    'nav.sports': 'Thể thao',
    'nav.arts': 'Sân khấu & Nghệ thuật',
    'nav.family': 'Gia đình',
    'nav.cities': 'Thành phố',
    'nav.myTicketsDropdown': 'Vé của tôi',
    'nav.membershipCard': 'Thẻ thành viên',
    'nav.myEvents': 'Sự kiện của tôi',
    'nav.myAccount': 'Tài khoản của tôi',
    'nav.signOut': 'Đăng xuất',

    // Hero / Search
    'hero.promoted': 'ĐƯỢC ĐỀ XUẤT',
    'hero.findTickets': 'Tìm vé',
    'hero.search': 'Tìm kiếm',
    'hero.location': 'ĐỊA ĐIỂM',
    'hero.locationPlaceholder': 'Thành phố hoặc Mã bưu điện',
    'hero.dates': 'NGÀY',
    'hero.datesAll': 'Tất cả ngày',
    'hero.datesToday': 'Hôm nay',
    'hero.datesWeekend': 'Cuối tuần này',
    'hero.datesMonth': 'Tháng này',
    'hero.searchLabel': 'TÌM KIẾM',
    'hero.searchPlaceholder': 'Nghệ sĩ, sự kiện hoặc địa điểm',

    // Deal Banners
    'deal.conference': 'HỘI NGHỊ',
    'deal.summitSubtitle': 'Nơi khởi nguồn đổi mới sáng tạo.',

    // Categories translation
    'category.Concerts': 'Âm nhạc',
    'category.Sports': 'Thể thao',
    'category.Arts & Theater': 'Nghệ thuật & Sân khấu',
    'category.Family': 'Gia đình',
    'category.Comedy': 'Hài kịch',
    'category.More': 'Thêm nữa',

    // Homepage categories & events
    'home.browseCategory': 'Duyệt theo danh mục',
    'home.viewAll': 'Xem tất cả',
    'home.eventsLike': 'Sự kiện dành cho bạn',
    'home.whyBook': 'Tại sao nên đặt vé tại TicketHub?',
    'home.verifiedTickets': 'Vé đã xác thực',
    'home.verifiedDesc': 'Mọi vé đều chính hãng 100% và được bảo chứng bởi chúng tôi.',
    'home.quickCheckout': 'Thanh toán nhanh',
    'home.quickDesc': 'Giữ chỗ trong vài giây với quy trình giao dịch tối ưu hóa.',
    'home.support': 'Hỗ trợ 24/7',
    'home.supportDesc': 'Chúng tôi luôn sẵn sàng hỗ trợ trước, trong và sau sự kiện.',

    // Common action buttons
    'action.seeTickets': 'Xem vé',
    'action.seePasses': 'Xem thẻ',
    'action.seeDeals': 'Xem ưu đãi',
    'action.learnMore': 'Tìm hiểu thêm',

    // Footer
    'footer.desc': 'TicketHub là nền tảng bán vé hàng đầu thế giới, kết nối hàng triệu khán giả đến với những trải nghiệm trực tiếp yêu thích.',
    'footer.colDiscover': 'Khám phá',
    'footer.colLinks': 'Liên kết hữu ích',
    'footer.colAbout': 'Về chúng tôi',
    'footer.linkHelp': 'Trung tâm trợ giúp',
    'footer.linkSell': 'Bán vé trên TicketHub',
    'footer.linkAccount': 'Tài khoản của tôi',
    'footer.linkContact': 'Liên hệ',
    'footer.linkStory': 'Câu chuyện của chúng tôi',
    'footer.linkCareers': 'Tuyển dụng',
    'footer.linkPress': 'Báo chí',
    'footer.linkInnovation': 'Sáng tạo',
    'footer.copyright': '© 2026 TicketHub Institutional. Bảo lưu mọi quyền.',
    'footer.terms': 'Điều khoản dịch vụ',
    'footer.privacy': 'Chính sách bảo mật',
    'footer.cookie': 'Chính sách Cookie'
  },
  Eng: {
    // Navigation Header
    'nav.organizer': 'Register as Organizer',
    'nav.myTickets': 'My Tickets',
    'nav.account': 'Account',
    'nav.signIn': 'Sign In/Register',
    'nav.concerts': 'Concerts',
    'nav.sports': 'Sports',
    'nav.arts': 'Arts, Theater & Comedy',
    'nav.family': 'Family',
    'nav.cities': 'Cities',
    'nav.myTicketsDropdown': 'My Tickets',
    'nav.membershipCard': 'Membership Card',
    'nav.myEvents': 'My Events',
    'nav.myAccount': 'My Account',
    'nav.signOut': 'Sign Out',

    // Hero / Search
    'hero.promoted': 'PROMOTED',
    'hero.findTickets': 'Find Tickets',
    'hero.search': 'Search',
    'hero.location': 'LOCATION',
    'hero.locationPlaceholder': 'City or Zip Code',
    'hero.dates': 'DATES',
    'hero.datesAll': 'All Dates',
    'hero.datesToday': 'Today',
    'hero.datesWeekend': 'This Weekend',
    'hero.datesMonth': 'This Month',
    'hero.searchLabel': 'SEARCH',
    'hero.searchPlaceholder': 'Artist, Event or Venue',

    // Deal Banners
    'deal.conference': 'CONFERENCE',
    'deal.summitSubtitle': 'The home for innovation.',

    // Categories translation
    'category.Concerts': 'Concerts',
    'category.Sports': 'Sports',
    'category.Arts & Theater': 'Arts & Theater',
    'category.Family': 'Family',
    'category.Comedy': 'Comedy',
    'category.More': 'More',

    // Homepage categories & events
    'home.browseCategory': 'Browse by Category',
    'home.viewAll': 'View All',
    'home.eventsLike': 'Events You Might Like',
    'home.whyBook': 'Why Book with TicketHub?',
    'home.verifiedTickets': 'Verified Tickets',
    'home.verifiedDesc': 'Every ticket is 100% authentic and backed by our guarantee.',
    'home.quickCheckout': 'Quick Checkout',
    'home.quickDesc': 'Secure your seats in seconds with our optimized flow.',
    'home.support': 'Support 24/7',
    'home.supportDesc': "We're here to help you before, during, and after the show.",

    // Common action buttons
    'action.seeTickets': 'See Tickets',
    'action.seePasses': 'See Passes',
    'action.seeDeals': 'See Deals',
    'action.learnMore': 'Learn More',

    // Footer
    'footer.desc': "TicketHub is the world's leading ticketing platform, connecting millions of fans to the live experiences they love.",
    'footer.colDiscover': 'Discover',
    'footer.colLinks': 'Helpful Links',
    'footer.colAbout': 'About Us',
    'footer.linkHelp': 'Help Center',
    'footer.linkSell': 'Sell on TicketHub',
    'footer.linkAccount': 'My Account',
    'footer.linkContact': 'Contact Us',
    'footer.linkStory': 'Our Story',
    'footer.linkCareers': 'Careers',
    'footer.linkPress': 'Press',
    'footer.linkInnovation': 'Innovation',
    'footer.copyright': '© 2026 TicketHub Institutional. All rights reserved.',
    'footer.terms': 'Terms of Service',
    'footer.privacy': 'Privacy Policy',
    'footer.cookie': 'Cookie Policy'
  }
};
