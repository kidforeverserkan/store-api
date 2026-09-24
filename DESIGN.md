# Design system — Store API storefront

Scope: every storefront page (`src/main/resources/templates/**`),
`src/main/resources/static/css/style.css` and the page scripts in
`src/main/resources/static/js/`. The approved visual reference is
`design-reference/store-api-approved-design.png`; where this document and the
reference disagree, the reference wins and this file gets updated.

## Direction

Premium, dark, cinematic, product-first. The structure comes from the
reference's e-commerce layout (header → hero → category reel → featured
products; product page with gallery, info and assurance column), the colour
from a deep-navy "night showroom", and the signature motion from a category
reel that moves on its own. Product photography is the hero material; UI
chrome stays quiet so photos and prices lead.

**Truth rule:** only real data is shown — names, descriptions, prices,
categories, cart, orders, payment status, service health. No ratings,
reviews, testimonials, stock, shipping/return promises or company claims.
Layout slots the reference fills with such content are omitted or filled
with true statements (e.g. "Secure payments — powered by Stripe").

## Colour (dark only)

Tokens live on `:root`; `color-scheme: dark`, no light theme.

| Token | Value | Role |
|---|---|---|
| `--color-bg` / `--color-bg-2` | `#060a14` / `#0a1122` | Page ground; alternate section ground |
| `--color-surface` → `-3` | `#0d1628` → `#192641` | Cards, panels, inputs, raised layers |
| `--color-border` / `-strong` | 14% / 28% slate-blue | Hairlines; control borders |
| `--color-text` | `#eef2fb` | Primary text |
| `--color-text-muted` / `-faint` | `#a6b1c6` / `#7d8aa2` | Secondary / tertiary (both ≥ 4.5:1 on every surface) |
| `--color-blue` / `-hover` | `#1d63ed` / `#2c72f7` | Filled actions (white label ≥ 4.5:1) |
| `--color-blue-bright` | `#5a95ff` | Blue text and icons on the dark ground |
| `--color-blue-tint` / `-glow` | 12% / 45% blue | Selected backgrounds; glow shadows |
| `--color-success` / `--color-danger` | `#3ecf8e` / `#ff7b7b` | Server online / offline, errors |
| `--tone-*` | per category | Only behind emoji fallbacks, mixed into the surface; never text |

Electric blue is the one accent: primary buttons, the cart button on cards,
active chips, the active reel card's outline, focus rings, the brand mark.
Glow (`--glow-blue`) is reserved for filled blue controls and the active reel
card — never for text or containers.

## Typography

- **Manrope** (400–800) for everything: display, headings, UI, body.
  Headlines are heavy (800) and tightly tracked (−0.03em); body is 16px/1.6.
- **IBM Plex Mono** only for code-ish tokens (the tech-stack chips on About).
- Scale: hero `clamp(2.6rem, 5.6vw, 4.6rem)`; page title `clamp(1.9rem, 3.5vw, 2.6rem)`;
  section title `clamp(1.7rem, 3vw, 2.3rem)`; card name ~1rem; prices 800
  weight with tabular numerals.
- Eyebrows (small uppercase blue labels, 0.72rem, 0.2em tracking) appear only
  above the hero headline and major home-page sections, as in the reference.

## Spacing, radius, depth

- Spacing scale `--space-1` (0.5rem) … `--space-8` (6.5rem); container max
  76rem with a 1.5rem gutter (1rem under 640px).
- Radii: 8 / 12 / 18 / 24px (`--radius-sm` … `--radius-xl`); pills are 999px.
- Depth comes from stepped surface lightness plus soft, offset shadows
  (`--shadow-card`, `--shadow-lift`); lifted cards also gain a faint blue ring.

## Components

- **Header** (sticky, translucent navy with blur): brand mark, Shop /
  Categories / About / Contact (Shop and Categories both open `/shop`, where
  categories are picked with chips), search, DKK/EUR selector, Account
  (Sign in → /login, Account → /orders), cart icon with badge, live status
  pill. Under 1100px the nav, search and status move into a menu panel
  behind a button.
- **Brand mark**: a shopping bag carrying `</>` (store + API), blue stroke
  with a soft glow; wordmark "Store **API**".
- **Buttons**: `.btn--primary` (filled blue + glow), `.btn--outline`,
  `.btn--ghost`, `.btn--lg`; round `.icon-btn` for reel controls; the product
  card's round blue cart button. Press state `scale(0.98)`.
- **Hero**: full-width; the product photo sits on the right and dissolves
  into the navy on its left and bottom edges (CSS mask) so its frame never
  reads; headline, lede, two CTAs, three true trust items, a product chip
  with the live price, scroll cue. The photo, product text and chip belong
  to the **featured-product reel** (see Motion), with Pause/Play and Next
  controls.
- **Category reel**: see Motion. Cards are photos with a dark gradient, an
  emoji category icon in a glass circle, name, a factual descriptor with the
  live product count, and a blue arrow on the active card. Each card opens
  its category on `/shop?category=…`.
- **Product card**: 4:3 visual (photo, or emoji on its category tone),
  category (emoji + name), name, price, round add-to-cart button. Hover/focus:
  lift −4px, blue ring, photo zoom 1.06.
- **Product page**: breadcrumb; 3 columns (stage | info | assurance) → 2 under
  1200px → 1 under 900px. Info: category badge, title, description, price,
  "Charged in DKK/EUR" note, quantity stepper + large add-to-cart, tabs
  (Description / Details / Payment — all real data), related products. The
  large price is unit price × quantity and updates as soon as the quantity
  changes; above 1 a small line shows "2 × €16.08 each".
- **Cart**: one row per item (photo, name, unit price, quantity, line total,
  Remove). The photo and the name both link to the product page (the photo
  link is skipped by keyboard and screen readers, since the name link goes to
  the same place).
- **Sign-in prompt** (`login-prompt.js`): shopping needs an account, so a
  signed-out Add to cart (card or product page) opens a native `<dialog>`
  instead of creating a cart: "Log in to shop", "Please log in to add items
  to your cart.", **Log in** (primary) and **Create account** (outline), both
  returning to the current page. Surface panel with the same blue corner
  glow as the account card; closes on Esc, ×, or a backdrop click.
- **Account** (`/orders`): overview card (avatar initial, name, email, order
  stats, actions) beside the order history. Normal accounts get an
  **Edit profile** button that swaps the order history for an edit panel
  (`#edit-profile`): Personal information (name, email), Security (current,
  new and confirm password), Delete account (two-step confirmation; refused
  with a clear message for accounts that have orders) and Back to account.
  The shared demo account instead shows "Public demo account" as its eyebrow
  and a lock notice, "This shared demo account can't be modified.", with no
  Edit profile.
- **Shop**: category chips (`aria-pressed`, synced to `?category=`), result
  count (`role=status`), empty state with "Clear search and filters".
- **Search**: header field; on /shop it filters live (every word must match
  name, description or category) and syncs `?q=`; elsewhere it opens
  `/shop?q=…`.

## Product imagery

- Photos are AI-generated originals for this project, keyed by product id in
  `static/js/product-visuals.js` (`PRODUCT_IMAGES`, with the expected name as
  a guard). Files: `static/images/products/product-{id}-{slug}-{width}.webp`,
  two widths each, served with `srcset`/`sizes` and fixed `width`/`height`.
- Every photo is decorative (`alt=""`, container `aria-hidden`): the product
  name is always text beside it.
- Products without a photo use the emoji map in the same file on a soft
  category tone. Emoji are for products and categories only, never for
  interface controls — those use inline SVG (1.7–2px round strokes).
- Source PNGs live in `product-image-originals/` and are never shipped.
- To add a photo: export WebP at the two widths into `images/products/` and
  add one `PRODUCT_IMAGES` entry.
- Book covers (31–40, except 38) are portrait originals. Their WebPs are 4:3
  (480/960 wide): the whole cover at full height in the centre, with the
  sides continuing the same photo blurred and dimmed, so landscape frames
  never slice a cover. Products 3, 4, 11, 21, 23, 24 and 52 are too tall for
  a 4:3 frame and use the same treatment; other square photos are 400/720
  WebPs with thin edge slivers from the source sheets trimmed off.
- All 58 products have a photo. The emoji map remains the fallback for any
  product whose id/name has no matching entry.

## Motion

One authored moment per page, all inside
`@media (prefers-reduced-motion: no-preference)`:

- **Home**: hero copy rises; the photo settles in, floats (±8px over 9s) and
  leans a few pixels toward a fine pointer (`motion.js`).
- **Featured-product reel** (hero, `featured-product-reel.js`): advances
  every 6s through five photographed products (headset → coffee maker →
  book → dumbbells → lamp → headset); the incoming photo glides in as the
  outgoing one drifts and fades, and the text panel swaps in step. Next
  steps forward.
- **Category reel** (signature, `category-reel.js`): advances by itself every
  4.8s — the track slides one card over 1.1s on a cinematic ease while the
  active card scales to 1, brightens and takes the blue outline; inactive
  cards sit at 0.93 scale, 82% opacity. Five inert clones let the loop flow
  forward from Office back to Electronics without a rewind. Arrows move one
  card (presses during a movement are queued), swipe on touch, and keyboard
  focus on a card brings it to the front.
- **Autoplay rule (both reels)**: the active progress segment's fill
  animation is the timer, and a single "paused by the visitor" state decides
  whether it runs. **Only the Pause button** sets it; **Play** clears it and
  autoplay really restarts. Hover, focus, touch, swipes and the arrows never
  pause autoplay; the arrows just move on and restart the countdown.
  Separately, the timer rests while the tab is hidden or the reel is scrolled
  out of view, and carries on by itself when it's visible again. That is not
  a pause and never changes the Play/Pause state. Slide changes are
  announced to screen readers only while autoplay is off.
- **Product lists**: cards rise in once when scrolled into view (60ms
  stagger); a currency change re-prices without replaying it.
- **Product page**: the stage settles in; the photo floats and leans toward
  the pointer.
- Feedback: card lift, photo zoom, button press, add-to-cart check, cart
  badge pulse on an actual add.

Reduced motion: a global rule zeroes animation/transition durations and
nothing autoplays; the category reel becomes a manual, horizontally
scrollable row (the row scrolls, never the page) with arrows and no pause
button, and the featured-product reel hides its pause button but can still
be stepped with Next. Coarse pointers get no ambient floating.

## Responsive

Checked at 1440 / 1024 / 768 / 375 / 320px with no page-level horizontal
overflow (the reel section uses `overflow: clip`). Header collapses at
1100px; hero stacks at 900px with the photo behind a scrim; product grids go
to two columns under 640px and one under 380px; the reel shows one card at
80vw on phones.

## Accessibility

Skip link; landmarks (`header`, labelled `nav`s, `main`, `footer`);
breadcrumbs as ordered lists; visible 2px focus ring on every interactive
element (search uses a glow ring); descriptive labels on icon buttons
("Add Noise Cancelling Headset to cart", "Pause category animation"); the
reel is a labelled carousel region with grouped slides, clones `inert`; tabs
follow the WAI-ARIA pattern (arrow/Home/End keys); cart additions from cards
are announced via a polite live region; the sign-in prompt is a native modal
`<dialog>` labelled by its title (focus stays inside, Esc closes); the
product price is a polite live region; account form messages use
`role="status"`; the service status is always stated in text, never by
colour alone.

## Extending

- Keep blue the only accent; don't add a second accent or light theme
  without updating this file.
- New sections reuse `.section`, `.section-head`, `.section-title`.
- Never invent product facts, reviews or promises to fill a layout slot.
- Re-run `impeccable detect` (`.claude/skills/impeccable/`, a local Claude
  Code skill; `.claude/` isn't committed) after visual changes. Its type-hierarchy check misreads Thymeleaf templates (it can't
  resolve `/css/style.css` from the template folder); judge that one on the
  rendered page.
