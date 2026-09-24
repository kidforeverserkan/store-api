import { apiFetch, ApiError } from "/js/api.js";
import { createPricer, onCurrencyChange } from "/js/currency.js";
import { CATEGORIES, categoryBySlug } from "/js/product-visuals.js";
import { renderProductCard, revealOnScroll } from "/js/product-card.js";
import { matchesQuery } from "/js/search.js";

const statusEl = document.getElementById("shop-status");
const gridEl = document.getElementById("product-grid");
const filterEl = document.getElementById("category-filter");
const resultsEl = document.getElementById("shop-results");
const emptyEl = document.getElementById("shop-empty");
const emptyText = document.getElementById("shop-empty-text");
const searchInput = document.getElementById("site-search");

const params = new URLSearchParams(window.location.search);
let products = [];
let activeSlug = categoryBySlug(params.get("category")) ? params.get("category") : null;
let query = (params.get("q") || "").trim();

// Same matching rule as the header's results dropdown (search.js).
function visibleProducts() {
  const category = categoryBySlug(activeSlug);
  return products.filter((p) => (!category || p.categoryId === category.id) && matchesQuery(p, query));
}

function syncUrl() {
  const url = new URL(window.location.href);
  if (activeSlug) url.searchParams.set("category", activeSlug);
  else url.searchParams.delete("category");
  if (query) url.searchParams.set("q", query);
  else url.searchParams.delete("q");
  history.replaceState(null, "", url);
}

function renderFilter() {
  filterEl.textContent = "";
  const options = [{ slug: null, name: "All", emoji: null }, ...CATEGORIES];
  for (const option of options) {
    const count = option.slug
      ? products.filter((p) => p.categoryId === categoryBySlug(option.slug).id).length
      : products.length;
    const chip = document.createElement("button");
    chip.type = "button";
    chip.className = "category-chip";
    chip.setAttribute("aria-pressed", String(option.slug === activeSlug));
    if (option.emoji) {
      const icon = document.createElement("span");
      icon.setAttribute("aria-hidden", "true");
      icon.textContent = option.emoji;
      chip.append(icon);
    }
    chip.append(option.name);
    const countEl = document.createElement("span");
    countEl.className = "category-chip__count";
    countEl.textContent = String(count);
    chip.append(countEl);
    chip.addEventListener("click", () => {
      activeSlug = option.slug;
      syncUrl();
      renderFilter();
      renderProducts();
    });
    filterEl.append(chip);
  }
  filterEl.hidden = false;
}

function describeResults(shown) {
  const category = categoryBySlug(activeSlug);
  const where = category ? ` in ${category.name}` : "";
  if (query) return `${shown} ${shown === 1 ? "result" : "results"} for “${query}”${where}`;
  return `${shown} ${shown === 1 ? "product" : "products"}${where}`;
}

async function renderProducts({ animate = true } = {}) {
  const pricer = await createPricer();
  const list = visibleProducts();
  gridEl.textContent = "";
  gridEl.classList.toggle("is-static", !animate);
  list.forEach((product, index) => gridEl.append(renderProductCard(product, pricer, { headingLevel: 2, index })));
  revealOnScroll(gridEl.children);

  resultsEl.textContent = describeResults(list.length);
  gridEl.hidden = list.length === 0;
  emptyEl.hidden = list.length !== 0;
  if (list.length === 0) {
    emptyText.textContent = query
      ? `Nothing matches “${query}”. Try a different word, or clear the search.`
      : "There are no products in this category yet.";
  }
}

function initSearch() {
  if (!searchInput) return;
  searchInput.value = query;
  let timer = null;
  searchInput.addEventListener("input", () => {
    clearTimeout(timer);
    timer = setTimeout(() => {
      query = searchInput.value.trim();
      syncUrl();
      renderProducts({ animate: false });
    }, 120);
  });
  // Already on the shop page: filter in place instead of reloading.
  searchInput.form?.addEventListener("submit", (event) => {
    event.preventDefault();
    query = searchInput.value.trim();
    syncUrl();
    renderProducts({ animate: false });
  });
}

document.getElementById("shop-clear")?.addEventListener("click", () => {
  query = "";
  activeSlug = null;
  if (searchInput) searchInput.value = "";
  syncUrl();
  renderFilter();
  renderProducts();
});

async function loadProducts() {
  try {
    products = await apiFetch("/products");

    if (!products || products.length === 0) {
      statusEl.textContent = "No products available right now.";
      return;
    }

    renderFilter();
    gridEl.classList.add("reveal-ready");
    await renderProducts();
    statusEl.hidden = true;
  } catch (err) {
    if (err instanceof ApiError) {
      statusEl.textContent = "Couldn't load products. Please try again shortly.";
    } else {
      statusEl.textContent = "Couldn't reach the server. Check your connection and try again.";
    }
  }
}

// A currency change only re-prices; it must not replay the entrance motion.
onCurrencyChange(() => {
  if (products.length > 0) renderProducts({ animate: false });
});
initSearch();
loadProducts();
