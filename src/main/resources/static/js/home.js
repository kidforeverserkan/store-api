import { apiFetch } from "/js/api.js";
import { createPricer, onCurrencyChange } from "/js/currency.js";
import { CATEGORIES, createProductImg, hasProductImage } from "/js/product-visuals.js";
import { renderProductCard, revealOnScroll } from "/js/product-card.js";
import { initCategoryReel } from "/js/category-reel.js";
import { initFeaturedReel } from "/js/featured-product-reel.js";
import { attachPointerDepth } from "/js/motion.js";

const hero = document.querySelector(".hero");
const reelRoot = document.querySelector(".category-reel");
const reelTrack = document.getElementById("category-track");
const reelProgress = document.getElementById("category-progress");
const featuredGrid = document.getElementById("featured-grid");
const featuredSection = document.getElementById("featured");

let products = [];
let featuredReel = null;

// The photographed products, one per category, in category order
// (Headset, Coffee Maker, Book, Dumbbells, Lamp).
function photographedProducts() {
  return CATEGORIES
    .map((c) => products.find((p) => p.id === c.photoProductId))
    .filter((p) => p && hasProductImage(p));
}

// Hero: the headset photo is in the page markup (above the fold, eager);
// the featured-product reel adds the others and rotates through them.
async function initHero() {
  const featured = photographedProducts();
  if (featured.length === 0) return;
  featuredReel = initFeaturedReel({ hero, products: featured, pricer: await createPricer() });
}

function renderReel() {
  reelTrack.textContent = "";
  reelProgress.textContent = "";
  CATEGORIES.forEach((category, index) => {
    const count = products.filter((p) => p.categoryId === category.id).length;
    const photoProduct = products.find((p) => p.id === category.photoProductId);

    const li = document.createElement("li");
    li.className = "reel-card";
    li.dataset.name = category.name;
    li.setAttribute("role", "group");
    li.setAttribute("aria-roledescription", "slide");
    li.setAttribute("aria-label", `${index + 1} of ${CATEGORIES.length}: ${category.name}`);

    const link = document.createElement("a");
    link.className = "reel-card__link";
    link.href = `/shop?category=${category.slug}`;

    const media = document.createElement("div");
    media.className = "reel-card__media";
    media.setAttribute("aria-hidden", "true");
    const img = photoProduct && createProductImg(photoProduct, { sizes: "(max-width: 640px) 80vw, 420px" });
    if (img) {
      // The reel is right under the hero and every card slides into view
      // within seconds — lazy loading would show an empty card mid-motion.
      img.loading = "eager";
      media.append(img);
    }
    else media.classList.add("product-tone", `product-tone--${category.slug}`);

    const body = document.createElement("div");
    body.className = "reel-card__body";
    const icon = document.createElement("span");
    icon.className = "reel-card__icon";
    icon.setAttribute("aria-hidden", "true");
    icon.textContent = category.emoji;
    const text = document.createElement("span");
    text.className = "reel-card__text";
    const name = document.createElement("span");
    name.className = "reel-card__name";
    name.textContent = category.name;
    const desc = document.createElement("span");
    desc.className = "reel-card__desc";
    desc.textContent = count > 0 ? `${category.descriptor} · ${count} products` : category.descriptor;
    text.append(name, desc);
    const arrow = document.createElement("span");
    arrow.className = "reel-card__go";
    arrow.setAttribute("aria-hidden", "true");
    arrow.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14M13 6l6 6-6 6"/></svg>';
    body.append(icon, text, arrow);

    link.append(media, body);
    li.append(link);
    reelTrack.append(li);

    const seg = document.createElement("span");
    seg.className = "reel-progress";
    seg.innerHTML = '<span class="reel-progress__fill"></span>';
    reelProgress.append(seg);
  });
  initCategoryReel(reelRoot);
}

// Featured: the products that have photography, one per category.
async function renderFeatured() {
  const featured = photographedProducts();
  if (featured.length === 0) {
    featuredSection.hidden = true;
    return;
  }
  const pricer = await createPricer();
  featuredGrid.textContent = "";
  featured.forEach((product, index) => featuredGrid.append(renderProductCard(product, pricer, { index })));
  featuredGrid.classList.add("reveal-ready");
  revealOnScroll(featuredGrid.children);
}

async function load() {
  try {
    products = await apiFetch("/products");
  } catch {
    products = []; // reel still renders from the category list, without counts
  }
  renderReel();
  await Promise.all([initHero(), renderFeatured()]);
}

attachPointerDepth(hero, hero.querySelector(".hero__media"));

onCurrencyChange(async () => {
  if (products.length === 0) return;
  featuredReel?.reprice(await createPricer());
  featuredGrid.classList.add("is-static");
  renderFeatured();
});
load();
