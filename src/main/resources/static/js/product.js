import { addProductToCart, apiFetch, ApiError } from "/js/api.js";
import { updateCartBadge } from "/js/nav.js";
import { createPricer, formatMinorUnits, loadRates, onCurrencyChange, toMinorUnits } from "/js/currency.js";
import { categoryById, createVisual, hasProductImage } from "/js/product-visuals.js";
import { renderProductCard, revealOnScroll } from "/js/product-card.js";
import { attachPointerDepth } from "/js/motion.js";
import { requireLoginToShop } from "/js/login-prompt.js";

const productId = document.body.dataset.productId;

const statusEl = document.getElementById("product-status");
const detailEl = document.getElementById("product-detail");
const visualEl = document.getElementById("product-visual");
const crumbCategory = document.getElementById("crumb-category");
const crumbName = document.getElementById("crumb-name");
const badgeEl = document.getElementById("product-category");
const nameEl = document.getElementById("product-name");
const priceEl = document.getElementById("product-price");
const unitPriceEl = document.getElementById("product-unit-price");
const currencyNoteEl = document.getElementById("product-currency-note");
const descEl = document.getElementById("product-desc");
const longDescEl = document.getElementById("product-long-desc");
const detailsEl = document.getElementById("product-details");
const qtyInput = document.getElementById("product-qty");
const addButton = document.getElementById("add-to-cart");
const addLabelEl = addButton.querySelector(".btn__label");
const addStatusEl = document.getElementById("add-to-cart-status");
const relatedSection = document.getElementById("related");
const relatedGrid = document.getElementById("related-grid");

let product = null;
let related = [];
let resetTimer = null;

let pricer = null;

// Display only: the price for the chosen quantity, built the same way as the
// cart and Stripe (converted unit price × quantity). The server still
// computes the real order amounts at checkout.
function renderLinePrice() {
  if (!product || !pricer) return;
  const quantity = clampQty(qtyInput.value);
  priceEl.textContent = pricer.format(pricer.line(product.price, quantity));
  unitPriceEl.textContent = `${quantity} × ${pricer.formatPrice(product.price)} each`;
  unitPriceEl.hidden = quantity === 1;
}

async function renderPrice() {
  pricer = await createPricer();
  renderLinePrice();
  currencyNoteEl.textContent = `Charged in ${pricer.currency} at checkout, securely via Stripe.`;
}

function addRow(term, value) {
  const dt = document.createElement("dt");
  dt.textContent = term;
  const dd = document.createElement("dd");
  dd.textContent = value;
  detailsEl.append(dt, dd);
}

// Only facts the API provides (plus the configured DKK/EUR conversion).
async function renderDetails() {
  detailsEl.textContent = "";
  const category = categoryById(product.categoryId);
  if (category) addRow("Category", category.name);
  addRow("Product number", `#${product.id}`);
  try {
    const rates = await loadRates();
    addRow("Price in DKK", formatMinorUnits(toMinorUnits(product.price, "DKK", rates), "DKK"));
    addRow("Price in EUR", formatMinorUnits(toMinorUnits(product.price, "EUR", rates), "EUR"));
  } catch {
    // Rates unavailable: the main price above still shows.
  }
}

function renderCategory() {
  const category = categoryById(product.categoryId);
  if (!category) return;
  const href = `/shop?category=${category.slug}`;
  crumbCategory.href = href;
  crumbCategory.textContent = category.name;
  crumbCategory.parentElement.hidden = false;
  badgeEl.href = href;
  badgeEl.querySelector("[data-icon]").textContent = category.emoji;
  badgeEl.querySelector("[data-name]").textContent = category.name;
  badgeEl.hidden = false;
}

async function renderRelated() {
  try {
    const all = await apiFetch(`/products?categoryId=${product.categoryId}`);
    // Same category, photographed products first, never the current one.
    related = all
      .filter((p) => p.id !== product.id)
      .sort((a, b) => Number(hasProductImage(b)) - Number(hasProductImage(a)))
      .slice(0, 4);
  } catch {
    related = [];
  }
  if (related.length === 0) return;
  const pricer = await createPricer();
  relatedGrid.textContent = "";
  related.forEach((p, index) => relatedGrid.append(renderProductCard(p, pricer, { index })));
  relatedGrid.classList.add("reveal-ready");
  revealOnScroll(relatedGrid.children);
  relatedSection.hidden = false;
}

async function loadProduct() {
  try {
    product = await apiFetch(`/products/${productId}`);

    document.title = `${product.name} — Store API`;
    nameEl.textContent = product.name;
    crumbName.textContent = product.name;
    descEl.textContent = product.description;
    longDescEl.textContent = product.description;
    visualEl.replaceChildren(createVisual(product, "product-visual", {
      sizes: "(max-width: 900px) 92vw, 560px",
      eager: true,
    }));
    renderCategory();
    await renderPrice();
    renderDetails();

    statusEl.hidden = true;
    detailEl.hidden = false;
    renderRelated();
  } catch (err) {
    if (err instanceof ApiError && err.status === 404) {
      statusEl.textContent = "This product doesn't exist or was removed.";
    } else {
      statusEl.textContent = "Couldn't load this product. Please try again shortly.";
    }
  }
}

// ----- quantity + add to cart ----------------------------------------------

function clampQty(value) {
  return Math.min(10, Math.max(1, Math.round(Number(value)) || 1));
}

document.querySelectorAll("[data-qty-step]").forEach((button) => {
  button.addEventListener("click", () => {
    qtyInput.value = String(clampQty(Number(qtyInput.value) + Number(button.dataset.qtyStep)));
    renderLinePrice();
  });
});
// Typing updates the price as soon as the field holds a number; leaving the
// field snaps it back into the 1–10 range.
qtyInput.addEventListener("input", renderLinePrice);
qtyInput.addEventListener("change", () => {
  qtyInput.value = String(clampQty(qtyInput.value));
  renderLinePrice();
});

async function addToCart() {
  if (!requireLoginToShop()) return;
  clearTimeout(resetTimer);
  const quantity = clampQty(qtyInput.value);
  addButton.disabled = true;
  addButton.classList.remove("is-added");
  addLabelEl.textContent = "Adding…";
  addStatusEl.textContent = "";

  try {
    await addProductToCart(productId, quantity);
    addButton.classList.add("is-added");
    addLabelEl.textContent = "Added";
    addStatusEl.textContent = quantity === 1 ? "Added to cart." : `Added ${quantity} to cart.`;
    await updateCartBadge();
    resetTimer = setTimeout(() => {
      addButton.classList.remove("is-added");
      addLabelEl.textContent = "Add to cart";
    }, 1800);
  } catch {
    addLabelEl.textContent = "Add to cart";
    addStatusEl.textContent = "Couldn't add this to your cart. Please try again.";
  } finally {
    addButton.disabled = false;
  }
}

// ----- tabs (WAI-ARIA tabs pattern) ----------------------------------------

function initTabs() {
  const tabs = [...document.querySelectorAll('[role="tab"]')];
  const select = (tab, focus = false) => {
    for (const t of tabs) {
      const selected = t === tab;
      t.setAttribute("aria-selected", String(selected));
      t.tabIndex = selected ? 0 : -1;
      document.getElementById(t.getAttribute("aria-controls")).hidden = !selected;
    }
    if (focus) tab.focus();
  };
  tabs.forEach((tab, i) => {
    tab.addEventListener("click", () => select(tab));
    tab.addEventListener("keydown", (event) => {
      const moves = { ArrowRight: 1, ArrowLeft: -1 };
      if (event.key in moves) {
        event.preventDefault();
        select(tabs[(i + moves[event.key] + tabs.length) % tabs.length], true);
      } else if (event.key === "Home" || event.key === "End") {
        event.preventDefault();
        select(event.key === "Home" ? tabs[0] : tabs[tabs.length - 1], true);
      }
    });
  });
}

addButton.addEventListener("click", addToCart);
attachPointerDepth(visualEl, visualEl);
initTabs();
onCurrencyChange(async () => {
  if (!product) return;
  renderPrice();
  if (related.length > 0) {
    const pricer = await createPricer();
    relatedGrid.classList.add("is-static");
    relatedGrid.textContent = "";
    related.forEach((p, index) => relatedGrid.append(renderProductCard(p, pricer, { index })));
    revealOnScroll(relatedGrid.children);
  }
});
loadProduct();
