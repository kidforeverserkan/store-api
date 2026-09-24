// The one product card used everywhere products are listed (home featured
// grid, shop grid, related products). Shows only real data: visual, category,
// name, price — plus an add-to-cart button that uses the existing cart API.
import { addProductToCart } from "/js/api.js";
import { announce, updateCartBadge } from "/js/nav.js";
import { categoryById, createVisual } from "/js/product-visuals.js";
import { requireLoginToShop } from "/js/login-prompt.js";

const CART_ICON = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M3 4h2l2.2 10.2a1.5 1.5 0 0 0 1.5 1.2h8.6a1.5 1.5 0 0 0 1.5-1.1L21 8H6.2"/><circle cx="9.5" cy="19.5" r="1.2"/><circle cx="17" cy="19.5" r="1.2"/></svg>';
const ERROR_ICON = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" aria-hidden="true"><path d="M12 7v6M12 16.5v.5"/></svg>';
const CHECK_ICON ='<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M5 12.5 10 17.5 19 7"/></svg>';

// Card image slot is ~260–340px wide on desktop, ~full width on phones.
const CARD_SIZES = "(max-width: 560px) 92vw, (max-width: 1024px) 45vw, 300px";

async function quickAdd(product, button) {
  if (!requireLoginToShop()) return;
  button.disabled = true;
  try {
    await addProductToCart(product.id);
    button.classList.add("is-added");
    button.innerHTML = CHECK_ICON;
    announce(`${product.name} added to cart.`);
    await updateCartBadge();
    setTimeout(() => {
      button.classList.remove("is-added");
      button.innerHTML = CART_ICON;
    }, 1600);
  } catch {
    // Visible, not just announced: a brief error state on the button.
    button.classList.add("is-error");
    button.innerHTML = ERROR_ICON;
    announce(`Couldn't add ${product.name} to your cart. Please try again.`);
    setTimeout(() => {
      button.classList.remove("is-error");
      button.innerHTML = CART_ICON;
    }, 2000);
  } finally {
    button.disabled = false;
  }
}

export function renderProductCard(product, pricer, { headingLevel = 3, index = 0, eager = false } = {}) {
  const li = document.createElement("li");
  li.className = "product-card";
  li.dataset.productId = String(product.id);
  li.style.setProperty("--i", String(Math.min(index, 11)));

  const link = document.createElement("a");
  link.className = "product-card__link";
  link.href = `/shop/${product.id}`;

  const media = createVisual(product, "product-card__media", { sizes: CARD_SIZES, eager });

  const body = document.createElement("div");
  body.className = "product-card__body";

  const category = categoryById(product.categoryId);
  if (category) {
    const tag = document.createElement("span");
    tag.className = "product-card__category";
    const icon = document.createElement("span");
    icon.setAttribute("aria-hidden", "true");
    icon.textContent = category.emoji;
    tag.append(icon, category.name);
    body.append(tag);
  }

  const name = document.createElement(`h${headingLevel}`);
  name.className = "product-card__name";
  name.textContent = product.name;
  body.append(name);

  link.append(media, body);

  const footer = document.createElement("div");
  footer.className = "product-card__footer";

  const price = document.createElement("span");
  price.className = "product-card__price";
  price.textContent = pricer.formatPrice(product.price);

  const add = document.createElement("button");
  add.type = "button";
  add.className = "product-card__add";
  add.setAttribute("aria-label", `Add ${product.name} to cart`);
  add.innerHTML = CART_ICON;
  add.addEventListener("click", () => quickAdd(product, add));

  footer.append(price, add);
  li.append(link, footer);
  return li;
}

// Cards rise in once as they scroll into view (motion-gated in CSS; with
// reduced motion or no IntersectionObserver they are simply visible).
let revealObserver = null;
export function revealOnScroll(elements) {
  const els = [...elements];
  if (!("IntersectionObserver" in window)) {
    els.forEach((el) => el.classList.add("is-visible"));
    return;
  }
  revealObserver ||= new IntersectionObserver((entries) => {
    for (const entry of entries) {
      if (entry.isIntersecting) {
        entry.target.classList.add("is-visible");
        revealObserver.unobserve(entry.target);
      }
    }
  }, { rootMargin: "0px 0px -8% 0px" });
  els.forEach((el) => revealObserver.observe(el));
}
