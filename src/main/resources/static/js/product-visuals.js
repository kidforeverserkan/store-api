// Presentation-only product visuals. Product names, prices and descriptions
// always come from the API; this module only decides which photo — or, when
// a product has none yet, which decorative emoji and background tone — is
// shown next to them. Nothing here is stored in, or sent to, the backend.
//
// Lookup order: product photo -> exact catalog name -> keyword rule ->
// category -> default emoji.

// Category ids match the categories seeded by the database (V5). Names,
// icons and descriptors are storefront presentation only; each descriptor
// summarises what the catalog actually contains in that category.
export const CATEGORIES = [
  { id: 1, slug: "electronics", name: "Electronics", emoji: "🎧", photoProductId: 10, descriptor: "Audio, screens, storage and more" },
  { id: 2, slug: "home", name: "Home & Kitchen", emoji: "☕", photoProductId: 22, descriptor: "Coffee, cooking and smart home" },
  { id: 3, slug: "books", name: "Books", emoji: "📚", photoProductId: 38, descriptor: "Software engineering classics" },
  { id: 4, slug: "fitness", name: "Fitness", emoji: "🏋️", photoProductId: 44, descriptor: "Strength and recovery at home" },
  { id: 5, slug: "office", name: "Office", emoji: "💡", photoProductId: 51, descriptor: "Desk setup and focus" },
];

// Product photography (AI-generated, original to this project), keyed by
// product id. `name` must match the API's product name too — if a database
// ever has different ids, a mismatched product falls back to its emoji
// instead of showing the wrong photo. To add a photo: put the WebP sizes in
// /images/products/ and add one entry here.
const PRODUCT_IMAGES = {
  // 3, 4, 11, 21, 23, 24 and 52 are taller than a 4:3 frame shows, so like
  // the book covers their WebPs are 4:3 canvases with the whole photo centred.
  1: { name: "Wireless Gaming Mouse", file: "product-1-wireless-gaming-mouse", widths: [400, 720], ratio: 1, focus: "50% 50%" },
  2: { name: "USB-C Hub 7-in-1", file: "product-2-usb-c-hub-7-in-1", widths: [400, 720], ratio: 1, focus: "50% 50%" },
  3: { name: "27-inch QHD Monitor", file: "product-3-27-inch-qhd-monitor", widths: [480, 960], ratio: 4 / 3, focus: "50% 50%" },
  4: { name: "24-inch Full HD Monitor", file: "product-4-24-inch-full-hd-monitor", widths: [480, 960], ratio: 4 / 3, focus: "50% 50%" },
  5: { name: "USB-C Docking Station", file: "product-5-usb-c-docking-station", widths: [400, 720], ratio: 1, focus: "50% 50%" },
  6: { name: "1080p Full HD Webcam", file: "product-6-1080p-full-hd-webcam", widths: [400, 720], ratio: 1, focus: "50% 50%" },
  7: { name: "USB Condenser Microphone", file: "product-7-usb-condenser-microphone", widths: [400, 720], ratio: 1, focus: "50% 50%" },
  8: { name: "Wireless Bluetooth Speaker", file: "product-8-wireless-bluetooth-speaker", widths: [400, 720], ratio: 1, focus: "50% 50%" },
  9: { name: "Wireless Earbuds", file: "product-9-wireless-earbuds", widths: [400, 720], ratio: 1, focus: "50% 50%" },
  10: { name: "Noise Cancelling Headset", file: "product-10-noise-cancelling-headset", widths: [400, 756], ratio: 756 / 500, focus: "50% 45%" },
  11: { name: "Portable Power Bank 20000mAh", file: "product-11-portable-power-bank-20000mah", widths: [480, 960], ratio: 4 / 3, focus: "50% 50%" },
  12: { name: "65W USB-C Laptop Charger", file: "product-12-65w-usb-c-laptop-charger", widths: [400, 720], ratio: 758 / 753, focus: "50% 50%" },
  13: { name: "Portable SSD 1TB", file: "product-13-portable-ssd-1tb", widths: [400, 720], ratio: 757 / 753, focus: "50% 50%" },
  14: { name: "Portable SSD 2TB", file: "product-14-portable-ssd-2tb", widths: [400, 720], ratio: 755 / 753, focus: "50% 50%" },
  15: { name: "External Hard Drive 2TB", file: "product-15-external-hard-drive-2tb", widths: [400, 720], ratio: 1, focus: "50% 50%" },
  16: { name: "Wi-Fi 6 Router", file: "product-16-wi-fi-6-router", widths: [400, 720], ratio: 1, focus: "50% 50%" },
  17: { name: "USB 3.0 Flash Drive 128GB", file: "product-17-usb-3-0-flash-drive-128gb", widths: [400, 720], ratio: 748 / 727, focus: "50% 50%" },
  18: { name: "Mechanical Keyboard", file: "product-18-mechanical-keyboard", widths: [400, 720], ratio: 763 / 727, focus: "50% 50%" },
  19: { name: "Wireless Keyboard and Mouse Set", file: "product-19-wireless-keyboard-and-mouse-set", widths: [400, 720], ratio: 754 / 727, focus: "50% 50%" },
  20: { name: "4K HDMI Cable", file: "product-20-4k-hdmi-cable", widths: [400, 720], ratio: 1, focus: "50% 50%" },
  21: { name: "Electric Kettle", file: "product-21-electric-kettle", widths: [480, 960], ratio: 4 / 3, focus: "50% 50%" },
  22: { name: "Programmable Coffee Maker", file: "product-22-programmable-coffee-maker", widths: [400, 756], ratio: 756 / 500, focus: "52% 50%" },
  23: { name: "Compact Air Fryer", file: "product-23-compact-air-fryer", widths: [480, 960], ratio: 4 / 3, focus: "50% 50%" },
  24: { name: "Personal Blender", file: "product-24-personal-blender", widths: [480, 960], ratio: 4 / 3, focus: "50% 50%" },
  25: { name: "Digital Kitchen Scale", file: "product-25-digital-kitchen-scale", widths: [400, 720], ratio: 761 / 768, focus: "50% 50%" },
  26: { name: "Smart LED Light Bulb", file: "product-26-smart-led-light-bulb", widths: [400, 720], ratio: 1, focus: "50% 50%" },
  27: { name: "Smart Plug 2-Pack", file: "product-27-smart-plug-2-pack", widths: [400, 720], ratio: 1, focus: "50% 50%" },
  28: { name: "Robot Vacuum Cleaner", file: "product-28-robot-vacuum-cleaner", widths: [400, 720], ratio: 759 / 756, focus: "50% 50%" },
  29: { name: "Electric Milk Frother", file: "product-29-electric-milk-frother", widths: [400, 720], ratio: 758 / 756, focus: "50% 50%" },
  30: { name: "Countertop Toaster", file: "product-30-countertop-toaster", widths: [400, 720], ratio: 758 / 756, focus: "50% 50%" },
  // Book covers 31–40 (except 38) are portrait originals set on a 4:3 canvas
  // so the whole cover shows in the landscape card and detail frames.
  31: { name: "Clean Code: A Handbook of Agile Software Craftsmanship", file: "product-31-clean-code", widths: [480, 960], ratio: 4 / 3, focus: "50% 50%" },
  32: { name: "Effective Java", file: "product-32-effective-java", widths: [480, 960], ratio: 4 / 3, focus: "50% 50%" },
  33: { name: "Spring in Action", file: "product-33-spring-in-action", widths: [480, 960], ratio: 4 / 3, focus: "50% 50%" },
  34: { name: "Learning SQL", file: "product-34-learning-sql", widths: [480, 960], ratio: 4 / 3, focus: "50% 50%" },
  35: { name: "Head First Design Patterns", file: "product-35-head-first-design-patterns", widths: [480, 960], ratio: 4 / 3, focus: "50% 50%" },
  36: { name: "Data Structures and Algorithms in Java", file: "product-36-data-structures-and-algorithms-in-java", widths: [480, 960], ratio: 4 / 3, focus: "50% 50%" },
  37: { name: "C++ Primer", file: "product-37-cpp-primer", widths: [480, 960], ratio: 4 / 3, focus: "50% 50%" },
  38: { name: "The Pragmatic Programmer", file: "product-38-the-pragmatic-programmer", widths: [320, 500], ratio: 1, focus: "42% 50%" },
  39: { name: "Designing Data-Intensive Applications", file: "product-39-designing-data-intensive-applications", widths: [480, 960], ratio: 4 / 3, focus: "50% 50%" },
  40: { name: "Web Development with HTML and CSS", file: "product-40-web-development-with-html-and-css", widths: [480, 960], ratio: 4 / 3, focus: "50% 50%" },
  41: { name: "Smart Fitness Tracker", file: "product-41-smart-fitness-tracker", widths: [400, 720], ratio: 757 / 761, focus: "50% 50%" },
  42: { name: "Digital Bathroom Scale", file: "product-42-digital-bathroom-scale", widths: [400, 720], ratio: 768 / 761, focus: "50% 50%" },
  43: { name: "Resistance Band Set", file: "product-43-resistance-band-set", widths: [400, 720], ratio: 768 / 761, focus: "50% 50%" },
  44: { name: "Adjustable Dumbbell Set", file: "product-44-adjustable-dumbbell-set", widths: [320, 482], ratio: 482 / 500, focus: "45% 55%" },
  45: { name: "Yoga Mat", file: "product-45-yoga-mat", widths: [400, 720], ratio: 1, focus: "50% 50%" },
  46: { name: "Massage Gun", file: "product-46-massage-gun", widths: [400, 720], ratio: 760 / 763, focus: "50% 50%" },
  47: { name: "Stainless Steel Sports Bottle", file: "product-47-stainless-steel-sports-bottle", widths: [400, 720], ratio: 1, focus: "50% 50%" },
  48: { name: "Smart Jump Rope", file: "product-48-smart-jump-rope", widths: [400, 720], ratio: 768 / 763, focus: "50% 50%" },
  49: { name: "Adjustable Monitor Stand", file: "product-49-adjustable-monitor-stand", widths: [400, 720], ratio: 720 / 692, focus: "50% 50%" },
  50: { name: "Ergonomic Laptop Stand", file: "product-50-ergonomic-laptop-stand", widths: [400, 720], ratio: 720 / 692, focus: "50% 50%" },
  51: { name: "LED Desk Lamp", file: "product-51-led-desk-lamp", widths: [320, 462], ratio: 462 / 500, focus: "42% 50%" },
  52: { name: "Monitor Arm", file: "product-52-monitor-arm", widths: [480, 960], ratio: 4 / 3, focus: "50% 50%" },
  53: { name: "Large Desk Mat", file: "product-53-large-desk-mat", widths: [400, 720], ratio: 768 / 759, focus: "50% 50%" },
  54: { name: "Wireless Presentation Remote", file: "product-54-wireless-presentation-remote", widths: [400, 720], ratio: 764 / 768, focus: "50% 50%" },
  55: { name: "Desktop Organizer", file: "product-55-desktop-organizer", widths: [400, 720], ratio: 760 / 768, focus: "50% 50%" },
  56: { name: "USB Desk Fan", file: "product-56-usb-desk-fan", widths: [400, 720], ratio: 764 / 768, focus: "50% 50%" },
  57: { name: "Document Scanner", file: "product-57-document-scanner", widths: [400, 720], ratio: 768 / 764, focus: "50% 50%" },
  58: { name: "Wireless Label Printer", file: "product-58-wireless-label-printer", widths: [400, 720], ratio: 768 / 764, focus: "50% 50%" },
};

// The photo for this product, or null when it has none.
export function productImage(product) {
  const entry = PRODUCT_IMAGES[product?.id];
  if (!entry || entry.name !== product?.name) return null;
  const largest = entry.widths[entry.widths.length - 1];
  return {
    src: `/images/products/${entry.file}-${largest}.webp`,
    srcset: entry.widths.map((w) => `/images/products/${entry.file}-${w}.webp ${w}w`).join(", "),
    width: largest,
    height: Math.round(largest / entry.ratio),
    focus: entry.focus,
  };
}

export function hasProductImage(product) {
  return productImage(product) !== null;
}

// An <img> for a product photo. Decorative on purpose (alt=""): the product
// name is always rendered as text right next to it.
export function createProductImg(product, { sizes = "100vw", eager = false } = {}) {
  const image = productImage(product);
  if (!image) return null;
  const img = document.createElement("img");
  img.src = image.src;
  img.srcset = image.srcset;
  img.sizes = sizes;
  img.width = image.width;
  img.height = image.height;
  img.alt = "";
  img.decoding = "async";
  img.loading = eager ? "eager" : "lazy";
  if (eager) img.fetchPriority = "high";
  img.style.objectPosition = image.focus;
  return img;
}

// The seeded catalog, grouped by category so each product also knows its
// tone (cart and order items only carry a name, not a category).
const CATALOG = {
  electronics: {
    "Wireless Gaming Mouse": "🖱️",
    "USB-C Hub 7-in-1": "🔌",
    "27-inch QHD Monitor": "🖥️",
    "24-inch Full HD Monitor": "🖥️",
    "USB-C Docking Station": "🔌",
    "1080p Full HD Webcam": "📷",
    "USB Condenser Microphone": "🎙️",
    "Wireless Bluetooth Speaker": "🔊",
    "Wireless Earbuds": "🎧",
    "Noise Cancelling Headset": "🎧",
    "Portable Power Bank 20000mAh": "🔋",
    "65W USB-C Laptop Charger": "🔌",
    "Portable SSD 1TB": "💽",
    "Portable SSD 2TB": "💽",
    "External Hard Drive 2TB": "💽",
    "Wi-Fi 6 Router": "📶",
    "USB 3.0 Flash Drive 128GB": "💾",
    "Mechanical Keyboard": "⌨️",
    "Wireless Keyboard and Mouse Set": "⌨️",
    "4K HDMI Cable": "🔌",
  },
  home: {
    "Electric Kettle": "🫖",
    "Programmable Coffee Maker": "☕",
    "Compact Air Fryer": "🍟",
    "Personal Blender": "🥤",
    "Digital Kitchen Scale": "⚖️",
    "Smart LED Light Bulb": "💡",
    "Smart Plug 2-Pack": "🔌",
    "Robot Vacuum Cleaner": "🧹",
    "Electric Milk Frother": "🥛",
    "Countertop Toaster": "🍞",
  },
  // Varied covers so the shelf doesn't look copy-pasted.
  books: {
    "Clean Code: A Handbook of Agile Software Craftsmanship": "📘",
    "Effective Java": "📕",
    "Spring in Action": "📗",
    "Learning SQL": "📙",
    "Head First Design Patterns": "📘",
    "Data Structures and Algorithms in Java": "📕",
    "C++ Primer": "📗",
    "The Pragmatic Programmer": "📙",
    "Designing Data-Intensive Applications": "📘",
    "Web Development with HTML and CSS": "📕",
  },
  fitness: {
    "Smart Fitness Tracker": "⌚",
    "Digital Bathroom Scale": "⚖️",
    "Resistance Band Set": "💪",
    "Adjustable Dumbbell Set": "🏋️",
    "Yoga Mat": "🧘",
    "Massage Gun": "💆",
    "Stainless Steel Sports Bottle": "💧",
    "Smart Jump Rope": "🪢",
  },
  office: {
    "Adjustable Monitor Stand": "🖥️",
    "Ergonomic Laptop Stand": "💻",
    "LED Desk Lamp": "💡",
    "Monitor Arm": "🖥️",
    "Large Desk Mat": "🖱️",
    "Wireless Presentation Remote": "📽️",
    "Desktop Organizer": "🗂️",
    "USB Desk Fan": "🌀",
    "Document Scanner": "📠",
    "Wireless Label Printer": "🏷️",
  },
};

const BY_NAME = new Map();
for (const [tone, products] of Object.entries(CATALOG)) {
  for (const [name, emoji] of Object.entries(products)) {
    BY_NAME.set(name, { emoji, tone });
  }
}

// For products added later that aren't in the catalog map. More specific
// words come first (e.g. "keyboard" before "mouse" for combo sets).
const KEYWORDS = [
  [/keyboard/i, "⌨️"],
  [/mouse/i, "🖱️"],
  [/laptop/i, "💻"],
  [/monitor|display|screen/i, "🖥️"],
  [/headphone|headset|earbud/i, "🎧"],
  [/phone/i, "📱"],
  [/camera|webcam/i, "📷"],
  [/speaker/i, "🔊"],
  [/coffee|espresso/i, "☕"],
  [/blender|smoothie/i, "🥤"],
  [/dumbbell|kettlebell/i, "🏋️"],
  [/yoga/i, "🧘"],
  [/notebook|journal/i, "📓"],
  [/lamp|bulb|light/i, "💡"],
  [/chair/i, "🪑"],
  [/book|guide|handbook/i, "📚"],
  [/cable|charger|hub|dock|plug/i, "🔌"],
];

const DEFAULT_EMOJI = "🛍️";

export function categoryById(id) {
  return CATEGORIES.find((c) => c.id === Number(id)) || null;
}

export function categoryBySlug(slug) {
  return CATEGORIES.find((c) => c.slug === slug) || null;
}

// Works with anything that has a name and (optionally) a categoryId — the
// product list/detail DTOs have both, cart and order items only a name.
export function productVisual(product) {
  const name = product?.name || "";
  const category = categoryById(product?.categoryId);
  const known = BY_NAME.get(name);
  const tone = category?.slug || known?.tone || "neutral";

  if (known) return { emoji: known.emoji, tone };

  const rule = KEYWORDS.find(([pattern]) => pattern.test(name));
  if (rule) return { emoji: rule[1], tone };

  return { emoji: category?.emoji || DEFAULT_EMOJI, tone };
}

// A decorative visual element: the product photo when one exists, otherwise
// the emoji on its category tone. Always aria-hidden — the product name
// shown next to it is what identifies the product.
export function createVisual(product, className, options = {}) {
  const { emoji, tone } = productVisual(product);
  const wrapper = document.createElement("div");
  wrapper.setAttribute("aria-hidden", "true");

  const img = createProductImg(product, options);
  if (img) {
    wrapper.className = `${className} product-media product-media--photo`;
    img.className = `${className}-img`;
    wrapper.append(img);
    return wrapper;
  }

  wrapper.className = `${className} product-media product-media--emoji product-tone product-tone--${tone}`;
  const glyph = document.createElement("span");
  glyph.className = `${className}-emoji`;
  glyph.textContent = emoji;
  wrapper.append(glyph);
  return wrapper;
}
