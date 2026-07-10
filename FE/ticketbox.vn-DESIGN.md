# Design System Inspired by Ticketbox — Ocean Blue & White Edition

## 1. Visual Theme & Atmosphere

Ticketbox embodies a vibrant, entertainment-focused digital marketplace that balances sophisticated event discovery with bold visual energy. This edition adopts a **light, airy Ocean Blue & White** aesthetic — clean, modern, and trustworthy — tailored to Vietnam's diverse event ecosystem. The visual language uses crisp white surfaces and refreshing blue tones to create a premium, accessible experience. Bright blue accents highlight featured events and calls-to-action, while white backgrounds keep content legible and inviting. The system emphasizes clarity and efficient navigation, supporting users in discovering, exploring, and purchasing tickets across multiple event categories seamlessly.

**Key Characteristics**
- Clean, light-first aesthetic with vibrant ocean-blue accent colors
- Event-centric imagery and card-based content layouts
- Bright, energetic primary blue (`#1677FF`, `#4096FF`) for CTAs and highlights
- Professional typography hierarchy supporting content scanning
- Smooth color transitions and interactive states
- Responsive, touch-friendly interface optimized for mobile discovery
- Vietnamese-language-first content structure with accessible Unicode support

## 2. Color Palette & Roles

### Primary
- **Brand White** (`#FFFFFF`): Primary background, card surfaces, dominant page backdrop
- **Brand Blue** (`#1677FF`): Primary call-to-action buttons, featured event badges, success indicators, navigation bar
- **Brand Blue Light** (`#4096FF`): Secondary highlights, accent strokes, interactive hover states, eyebrow labels
- **Brand Blue Pale** (`#E6F4FF`): Section backgrounds, hero overlays, featured area tints

### Accent Colors
- **Electric Pink** (`#EB2F96`): Category highlights, promotional badges, secondary CTAs
- **Deep Purple** (`#722ED1`): Featured section accents, category differentiation
- **Cyan Teal** (`#13C2C2`): Interactive elements, data visualization, alternative CTAs
- **Ocean Dark** (`#0958D9`): Deep blue for hover states, featured section gradient

### Interactive
- **Success Green** (`#52C41A`): Success messages, confirmed states, completion indicators
- **Warning Yellow** (`#FADB14`): Warning states, important notices, alert badges
- **Error Red** (`#D82826`): Error states, cancel actions, critical warnings
- **Error Red Alt** (`#F5222D`): Danger states, strong negative actions

### Neutral Scale
- **White** (`#FFFFFF`): Card backgrounds, page backdrop, input fields, overlays
- **Black** (`#000000`): Text on light backgrounds, strong contrast text
- **Neutral Light** (`#F0F5FF`): Subtle section backgrounds, dividers, disabled states
- **Neutral Mid** (`#A6A6B0`): Secondary text, placeholders, tertiary information
- **Neutral Dark** (`#515158`): Tertiary text, muted labels, low-emphasis content
- **Text Primary** (`#0A1428`): Headings, primary body text on white backgrounds
- **Text Secondary** (`#3D4A63`): Body text, card descriptions

### Surface & Borders
- **Surface Light** (`#F7FBFF`): Subtle background tint, light mode variant, hero area
- **Border Light** (`#BAD4FF`): Card borders, input borders
- **Border Default** (`#91CAFF`): Focused borders, interactive outlines

## 3. Typography Rules

### Font Family
**Primary:** Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", sans-serif

**Secondary:** Inter, "Noto Sans CJK", -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif (for Vietnamese content)

### Hierarchy

| Role | Font | Size | Weight | Line Height | Letter Spacing | Notes |
|------|------|------|--------|-------------|----------------|-------|
| Display Large | Inter | 32px | 600 | 40px | 0px | Hero titles, featured event promotions |
| Display Medium | Inter | 28px | 600 | 36px | 0px | Section headers, campaign titles |
| Heading 1 | Inter | 24px | 600 | 32px | 0px | Page titles, featured section headers |
| Heading 2 | Inter | 20px | 600 | 28px | 0px | Card titles, event names, subsection headers |
| Heading 3 | Inter | 18px | 600 | 24px | 0px | Event category headers, featured stars title |
| Body Large | Inter | 16px | 400 | 22px | 0px | Primary body text, event descriptions |
| Body | Inter | 14px | 400 | 20px | 0px | Default body text, card descriptions, navigation labels |
| Body Small | Inter | 12px | 400 | 16px | 0px | Secondary text, metadata, event details |
| Caption | Inter | 11px | 400 | 14px | 0px | Image captions, timestamps, helper text |
| Button | Inter | 14px | 500 | 20px | 0px | Button text, CTAs |
| Input | Inter | 11.2px | 400 | 12.88px | 0px | Form input placeholder and value text |
| Code | Inter Mono | 12px | 400 | 16px | 0px | Code blocks, event IDs, reference numbers |

### Principles
- **Hierarchy through weight:** Titles use 600, body defaults to 400 for clear visual priority
- **Generous line height:** Minimum 1.4x font size for readability on light backgrounds
- **Vietnamese support:** Font stack includes Noto Sans CJK for reliable CJK rendering
- **Semantic sizing:** Each size serves a specific content role; avoid arbitrary sizing
- **Accessibility-first:** Minimum 12px for body text, 14px for interactive elements
- **Mobile optimization:** Sizes remain legible at 375px viewport without reduction on small screens

## 4. Component Stylings

### Buttons

#### Button Primary
- **Background:** `#1677FF`
- **Text Color:** `#FFFFFF`
- **Font Size:** `14px`
- **Font Weight:** `500`
- **Padding:** `12px 24px`
- **Border Radius:** `4px`
- **Border:** `0px none`
- **Min Height:** `40px`
- **Hover State:** Background `#0958D9`, scale 1.02
- **Active State:** Background `#003EB3`, box-shadow `inset 0 2px 4px rgba(0,0,0,0.15)`
- **Disabled State:** Background `#F0F5FF`, Text Color `#A6A6B0`, cursor not-allowed

#### Button Secondary
- **Background:** `#FFFFFF`
- **Text Color:** `#1677FF`
- **Font Size:** `14px`
- **Font Weight:** `500`
- **Padding:** `12px 24px`
- **Border Radius:** `4px`
- **Border:** `1px solid #91CAFF`
- **Min Height:** `40px`
- **Hover State:** Background `#E6F4FF`, border-color `#4096FF`
- **Active State:** Background `#BAD4FF`, box-shadow `inset 0 2px 4px rgba(0,0,0,0.08)`

#### Button Ghost
- **Background:** `transparent`
- **Text Color:** `#1677FF`
- **Font Size:** `14px`
- **Font Weight:** `500`
- **Padding:** `12px 24px`
- **Border Radius:** `4px`
- **Border:** `1px solid #1677FF`
- **Min Height:** `40px`
- **Hover State:** Background `rgba(22, 119, 255, 0.08)`
- **Active State:** Background `rgba(22, 119, 255, 0.15)`

#### Button Outline (White Border on Blue BG)
- **Background:** `transparent`
- **Text Color:** `#FFFFFF`
- **Font Size:** `14px`
- **Font Weight:** `500`
- **Padding:** `9.6px 35.2px`
- **Border Radius:** `32px`
- **Border:** `1px solid #FFFFFF`
- **Min Height:** `37px`
- **Hover State:** Background `rgba(255, 255, 255, 0.15)`, box-shadow `0 0 8px rgba(255, 255, 255, 0.3)`
- **Active State:** Background `rgba(255, 255, 255, 0.25)`

### Cards & Containers

#### Event Card
- **Background:** `#FFFFFF`
- **Border Radius:** `12px`
- **Padding:** `0px` (image section), `16px` (content section)
- **Border:** `1px solid #BAD4FF`
- **Box Shadow:** `0 2px 8px rgba(22, 119, 255, 0.08)`
- **Image Border Radius:** `12px 12px 0 0`
- **Title Font Size:** `16px`, weight `600`, color `#0A1428`
- **Description Font Size:** `14px`, weight `400`, color `#515158`
- **Hover State:** Box-shadow `0 4px 16px rgba(22, 119, 255, 0.16)`, transform `translateY(-2px)`

#### Featured Section Container
- **Background:** `linear-gradient(135deg, #1677FF 0%, #0958D9 60%, #003EB3 100%)`
- **Padding:** `40px 24px`
- **Border Radius:** `0px`
- **Border:** `0px none`
- **Background Accent:** Soft white glow overlay for featured areas

#### Category Badge Card
- **Background:** `#FFFFFF` with `1px solid #91CAFF` border
- **Border Radius:** `12px`
- **Padding:** `16px`
- **Image Border Radius:** `50%`
- **Image Size:** `80px x 80px`
- **Title Font Size:** `14px`, weight `600`, color `#0A1428`
- **Icon Color:** `#1677FF`
- **Hover State:** Border-color `#1677FF`, box-shadow `0 0 16px rgba(22, 119, 255, 0.25)`

### Inputs & Forms

#### Text Input
- **Background:** `#FFFFFF`
- **Text Color:** `#0A1428`
- **Placeholder Color:** `#A6A6B0`
- **Font Size:** `11.2px`
- **Padding:** `1px 8px`
- **Border Radius:** `0px`
- **Border:** `0px none`
- **Height:** `32px`
- **Box Shadow:** `none`
- **Focus State:** Border `1px solid #1677FF`, box-shadow `0 0 4px rgba(22, 119, 255, 0.3)`

#### Search Input Wrapper
- **Background:** `#FFFFFF`
- **Border Radius:** `24px`
- **Padding:** `8px 16px`
- **Display:** Flex with icon prefix
- **Icon Color:** `#A6A6B0`
- **Focus Within:** Border `1px solid #1677FF`

#### Select/Dropdown
- **Background:** `#FFFFFF`
- **Text Color:** `#0A1428`
- **Font Size:** `14px`
- **Padding:** `8px 12px`
- **Border Radius:** `4px`
- **Border:** `1px solid #BAD4FF`
- **Min Height:** `36px`
- **Focus State:** Border-color `#1677FF`, box-shadow `0 0 4px rgba(22, 119, 255, 0.2)`

### Navigation

#### Top Navigation Bar
- **Background:** `#1677FF`
- **Height:** `64px`
- **Padding:** `0px 24px`
- **Display:** Flex, items center, space-between
- **Box Shadow:** `0 2px 8px rgba(22, 119, 255, 0.2)`

#### Navigation Logo
- **Font Size:** `24px`
- **Font Weight:** `700`
- **Color:** `#FFFFFF`

#### Navigation Link
- **Font Size:** `14px`
- **Font Weight:** `400`
- **Color:** `#FFFFFF`
- **Padding:** `8px 16px`
- **Border Radius:** `4px`
- **Hover State:** Background `rgba(255, 255, 255, 0.15)`
- **Active State:** Background `rgba(0, 0, 0, 0.12)`, border-bottom `2px solid #FFFFFF`

#### Secondary Navigation
- **Background:** `#0A1428`
- **Height:** `48px`
- **Padding:** `0px 24px`
- **Display:** Flex, items center, gap `32px`
- **Font Size:** `13px`
- **Font Weight:** `400`
- **Text Color:** `#FFFFFF`

### Badges

#### Status Badge Success
- **Background:** `#52C41A`
- **Text Color:** `#FFFFFF`
- **Font Size:** `12px`
- **Font Weight:** `500`
- **Padding:** `4px 8px`
- **Border Radius:** `4px`

#### Status Badge Warning
- **Background:** `#FADB14`
- **Text Color:** `#0A1428`
- **Font Size:** `12px`
- **Font Weight:** `500`
- **Padding:** `4px 8px`
- **Border Radius:** `4px`

#### Status Badge Error
- **Background:** `#D82826`
- **Text Color:** `#FFFFFF`
- **Font Size:** `12px`
- **Font Weight:** `500`
- **Padding:** `4px 8px`
- **Border Radius:** `4px`

#### Category Badge
- **Background:** `transparent`
- **Border:** `1px solid #1677FF`
- **Text Color:** `#1677FF`
- **Font Size:** `12px`
- **Font Weight:** `600`
- **Padding:** `6px 12px`
- **Border Radius:** `16px`

### Links

#### Text Link
- **Font Size:** `14px`
- **Font Weight:** `400`
- **Color:** `#1677FF`
- **Text Decoration:** `none`
- **Hover State:** Text-decoration `underline`, color `#0958D9`
- **Visited State:** Color `#722ED1`

#### Navigation Link (White Border on Blue BG)
- **Font Size:** `14px`
- **Font Weight:** `400`
- **Color:** `#FFFFFF`
- **Padding:** `9.6px 35.2px`
- **Border Radius:** `32px`
- **Border:** `1px solid #FFFFFF`
- **Hover State:** Background `rgba(255, 255, 255, 0.15)`, box-shadow `0 0 12px rgba(255, 255, 255, 0.3)`

## 5. Layout Principles

### Spacing System
- **Base Unit:** `4px`
- **Scale:** `4px, 8px, 12px, 16px, 20px, 24px, 28px, 32px, 40px, 56px, 64px, 80px`
- **Usage Context:**
  - `4px` – Micro spacing between adjacent elements, component internal padding
  - `8px` – Small spacing, form field padding, tight groups
  - `12px` – Standard button padding, compact component spacing
  - `16px` – Card content padding, standard horizontal padding
  - `20px` – Section margins, medium vertical spacing
  - `24px` – Standard margin between sections, card horizontal gaps
  - `32px` – Large gap between sections, layout grid column gaps
  - `40px` – Extra-large vertical spacing, featured section padding
  - `56px` – Feature section top padding
  - `64px` – Major section separation, hero area bottom padding
  - `80px` – Page-level vertical margins

### Grid & Container
- **Max Width:** `1200px` for desktop layouts, `100%` for mobile
- **Column Strategy:** 12-column grid at desktop, 4-column at tablet, 1-column responsive stacking on mobile
- **Container Padding:** `24px` horizontal on desktop, `16px` on tablet, `12px` on mobile
- **Section Patterns:**
  - **Hero Section:** Full viewport width, `64px` top/bottom padding, `linear-gradient(135deg, #1677FF, #0958D9)` background
  - **Content Section:** `#FFFFFF` or `#F7FBFF` background, Max-width container, `40px` top/bottom padding
  - **Feature Grid:** Variable grid (8 columns desktop, 4 tablet, 2 mobile) with `16px` gap
  - **Card Carousel:** Horizontal scroll with `16px` gap, snap alignment

### Whitespace Philosophy
- **Breathing Space:** Generous margins between sections prevent visual crowding and improve content hierarchy
- **Content Isolation:** Cards and containers surrounded by whitespace with light blue-tinted borders
- **Vertical Rhythm:** Consistent spacing multiples create predictable, scannable layout
- **Mobile Density:** Spacing reduces on mobile while maintaining hierarchy; minimum `12px` gutters

### Border Radius Scale
- **Sharp:** `0px` – Navigation bars, full-width sections, backgrounds
- **Subtle:** `4px` – Buttons, form inputs, small components
- **Rounded:** `12px` – Cards, image containers, featured elements
- **Pill:** `32px` – Outline buttons, badge pills, circular containers
- **Circle:** `50%` – Avatar images, category badge centers

## 6. Depth & Elevation

| Level | Treatment | Use |
|-------|-----------|-----|
| Flat | `box-shadow: none` | Text, flat backgrounds, primary navigation |
| Raised | `box-shadow: 0 2px 8px rgba(22, 119, 255, 0.08)` | Cards, standard containers, default state |
| Elevated | `box-shadow: 0 4px 16px rgba(22, 119, 255, 0.16)` | Card hover, dropdowns, modal overlays |
| High | `box-shadow: 0 8px 24px rgba(22, 119, 255, 0.2)` | Modal dialogs, tooltips, floating actions |
| Ultra | `box-shadow: 0 12px 32px rgba(22, 119, 255, 0.25)` | Modals with backdrops, priority overlays |

**Shadow Philosophy:**
Shadows use the brand blue color at low opacity to reinforce the color system and create a cohesive depth layer. Event cards receive a gentle blue-tinted raise on hover to indicate interactivity. Featured sections use stronger blue shadows to establish visual hierarchy. Elevation increases as components move forward in the z-axis (navigation > cards > modals). Interactive hover states employ shadow lift to confirm user interaction capability.

**Glow/Accent Shadows:**
- Featured blue sections: `box-shadow: 0 0 16px rgba(22, 119, 255, 0.3)` for accent borders
- Focus states: `box-shadow: 0 0 4px rgba(22, 119, 255, 0.4)` for input/button focus rings
- Category card hover: `box-shadow: 0 0 16px rgba(22, 119, 255, 0.25)` for interactive feedback

## 7. Do's and Don'ts

### Do
- **Use the blue primary (`#1677FF`) for all primary CTAs** – Ensure consistent conversion signals across the platform
- **Apply generous padding to buttons and inputs** – Minimum `12px` vertical, `24px` horizontal for desktop touch targets
- **Maintain white backgrounds for content sections** – `#FFFFFF` or `#F7FBFF` establishes visual clarity and readability
- **Group related events in card grids** – Use `16px` gaps and consistent `12px` border radius for cohesive groupings
- **Prioritize event imagery** – Ensure images are sharp, correctly cropped at `3:4` ratio for event cards, `16:9` for hero carousels
- **Use semantic status colors** – `#52C41A` for success, `#FADB14` for warnings, `#D82826` for errors
- **Support Vietnamese typography** – Include Noto Sans CJK in font stack for reliable CJK character rendering
- **Implement clear focus states** – All interactive elements require visible focus rings for keyboard accessibility
- **Test at 375px mobile viewport** – Ensure layouts reflow gracefully without horizontal scrolling

### Don't
- **Avoid using multiple colors for similar CTAs** – Don't mix primary blue, pink, and teal for the same action type; reserve accents for secondary/tertiary actions
- **Don't reduce font sizes below 12px for body text** – Maintain legibility especially for Vietnamese content with complex character shapes
- **Avoid thin border weights** – Use minimum `1px` borders; thin lines become invisible at mobile scales
- **Don't place dark text on dark backgrounds** – Ensure WCAG AA contrast (4.5:1) minimum; text on white must be `#0A1428` or darker
- **Avoid deep nesting of card shadows** – Limit shadow elevation to 2–3 levels; excessive shadows create visual noise
- **Don't override focus states for keyboard navigation** – Always provide visible focus indicators (outline, ring, or background change)
- **Avoid inconsistent border radius** – Use only the defined scale (`0px, 4px, 12px, 32px, 50%`); arbitrary radii fragment the design
- **Don't auto-play videos or animations on featured sections** – Respect user preference; provide play controls
- **Avoid cramped spacing on mobile** – Maintain minimum `16px` padding on cards and `24px` gutters even at 375px viewport

## 8. Responsive Behavior

### Breakpoints

| Name | Width | Key Changes |
|------|-------|-------------|
| Mobile | `320px–479px` | Single column, `12px` padding, `2px` card gap, body `13px`, buttons stack full-width |
| Mobile Large | `480px–767px` | Single column, `16px` padding, `8px` card gap, body `13px`, buttons 50% width |
| Tablet | `768px–1023px` | 2–4 columns, `20px` padding, `12px` card gap, body `14px`, buttons inline |
| Desktop | `1024px–1439px` | 8 columns, `24px` padding, `16px` card gap, body `14px`, max-width `1200px` |
| Desktop Large | `1440px+` | 12 columns, `32px` padding, `16px` card gap, full featured imagery, hero carousels visible |

### Touch Targets
- **Minimum size:** `44px x 44px` for all interactive elements (buttons, links, form inputs)
- **Recommended size:** `48px x 48px` for optimal touch accuracy on mobile
- **Spacing:** Minimum `8px` between adjacent touch targets to prevent mis-taps
- **Mobile buttons:** Full-width or `50%` width stacking on screens under `480px`
- **Icons:** Minimum `20px x 20px` for standalone icons; `24px x 24px` preferred

### Collapsing Strategy
- **Hero Carousel:** Visible full-width on desktop; on tablet/mobile, single-column vertical scroll with next/prev arrows
- **Navigation:** Top bar collapses to hamburger menu on screens under `768px`; secondary nav hides below fold
- **Card Grid:** 8 columns desktop → 4 tablet → 2 mobile large → 1 mobile; gap reduces from `16px` to `12px` to `8px`
- **Sidebars:** Sidebars move below main content on tablet; fully reflow as single column on mobile
- **Forms:** Full-width stacking on mobile; multi-column at tablet/desktop with `20px` gaps
- **Padding/Margins:** Horizontal padding reduces from `24px` (desktop) → `16px` (tablet) → `12px` (mobile)
- **Typography:** Font sizes remain constant; line-height may reduce from `1.5x` to `1.4x` on very small screens for density

## 9. Agent Prompt Guide

### Quick Color Reference
- **Primary CTA:** Brand Blue (`#1677FF`)
- **Secondary CTA:** Brand Blue Light (`#4096FF`)
- **Accent/Highlight:** Electric Pink (`#EB2F96`)
- **Background (Page):** White (`#FFFFFF`)
- **Background (Section):** Surface Light (`#F7FBFF`)
- **Background (Hero/Nav):** Brand Blue (`#1677FF`) → gradient to `#0958D9`
- **Text on White:** Text Primary (`#0A1428`)
- **Text Secondary on White:** Text Secondary (`#3D4A63`)
- **Text on Blue:** White (`#FFFFFF`)
- **Success:** Success Green (`#52C41A`)
- **Warning:** Warning Yellow (`#FADB14`)
- **Error:** Error Red (`#D82826`)
- **Border:** Border Light (`#BAD4FF`)
- **Placeholder/Muted:** Neutral Mid (`#A6A6B0`)

### Iteration Guide
1. **All primary actions use `#1677FF` with white text** – Never deviate; this is the conversion signal color
2. **Button height minimum `40px`, padding `12px 24px`** – Ensures touch accessibility; respect this minimum
3. **Card border radius exactly `12px`** – No exceptions; use `50%` only for circular elements
4. **Page backgrounds are `#FFFFFF` or `#F7FBFF`** – Use `#FFFFFF` for content areas, `#F7FBFF` for section alternation
5. **Hero and nav background is `#1677FF`** – Gradient `#1677FF → #0958D9` for hero; solid `#1677FF` for nav
6. **Typography hierarchy follows sizes exactly** – Display `32px`, Heading 1 `24px`, Body `14px`, Caption `11px`; infer missing sizes from nearest reference
7. **Spacing increments by `4px` or `8px`** – Never use `5px`, `7px`, or arbitrary values; maintain the scale
8. **Focus states require visible outlines** – Minimum `2px` outline or `4px` box-shadow in `rgba(22, 119, 255, 0.4)`; keyboard users depend on this
9. **Event card images are `3:4` aspect ratio** – Always crop and display at this ratio; enforce via CSS `aspect-ratio: 3/4`
10. **Navigation bar is `#1677FF`, secondary bar is `#0A1428`** – Color difference establishes hierarchy
11. **Mobile viewport minimum width `375px`** – Test all layouts at 375px and ensure no horizontal scrolling without user interaction
12. **Vietnamese text requires Noto Sans CJK in font stack** – Include `"Noto Sans CJK"` after Inter for reliable rendering
13. **Shadow opacity uses blue tint** – Base cards `rgba(22, 119, 255, 0.08)`, hovered `rgba(22, 119, 255, 0.16)`, modals `rgba(22, 119, 255, 0.2)`
14. **Hover states scale elements by `1.02` or lift via shadow** – Never use color-only hover states; provide visual feedback through transform or shadow
15. **Status badges use semantic colors only** – No custom badge colors; map all statuses to the four semantic colors defined
16. **Eyebrow labels use `#4096FF`** – Light blue on white backgrounds; on blue backgrounds use `rgba(255,255,255,0.8)`