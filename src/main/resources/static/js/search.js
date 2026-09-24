// Product search: one matching rule for the whole site, plus the header's
// results dropdown (a WAI-ARIA combobox). Results are real catalog products
// from GET /products, shown with their photo — or emoji fallback — name,
// category and current price. Picking one opens its product page.
import { apiFetch } from "/js/api.js";
import { createPricer } from "/js/currency.js";
import { CATEGORIES, createVisual } from "/js/product-visuals.js";

const MAX_RESULTS = 6;

function normalize(text) {
  return text.toLocaleLowerCase("en").normalize("NFD").replace(/[̀-ͯ]/g, "");
}

// Every word of the query must appear in the product's name, description or
// category name — so "usb ssd" or "coffee" narrow the list as you'd expect.
export function matchesQuery(product, query) {
  const words = normalize(query).split(/\s+/).filter(Boolean);
  if (words.length === 0) return true;
  const category = CATEGORIES.find((c) => c.id === product.categoryId);
  const haystack = normalize(`${product.name} ${product.description} ${category ? category.name : ""}`);
  return words.every((word) => haystack.includes(word));
}

// Name matches first (a search for "lamp" should lead with the lamp, not a
// product that merely mentions lighting), then catalog order.
function rank(products, query) {
  const first = normalize(query).split(/\s+/).filter(Boolean)[0] || "";
  const inName = (p) => (normalize(p.name).includes(first) ? 0 : 1);
  return products
    .filter((p) => matchesQuery(p, query))
    .sort((a, b) => inName(a) - inName(b));
}

let catalogPromise = null;
function loadCatalog() {
  catalogPromise ||= apiFetch("/products").catch((err) => {
    catalogPromise = null;
    throw err;
  });
  return catalogPromise;
}

export function initSearchPanel() {
  const form = document.querySelector(".site-search");
  const input = document.getElementById("site-search");
  const panel = document.getElementById("search-panel");
  if (!form || !input || !panel) return;

  const list = panel.querySelector(".search-panel__list");
  const empty = panel.querySelector(".search-panel__empty");
  const all = panel.querySelector(".search-panel__all");
  let active = -1;
  let timer = null;
  let renderToken = 0;

  const options = () => [...list.querySelectorAll('[role="option"]')];

  function open() {
    panel.hidden = false;
    input.setAttribute("aria-expanded", "true");
  }

  function close() {
    panel.hidden = true;
    input.setAttribute("aria-expanded", "false");
    input.removeAttribute("aria-activedescendant");
    active = -1;
  }

  function setActive(index) {
    const opts = options();
    if (opts.length === 0) return;
    active = (index + opts.length) % opts.length;
    opts.forEach((opt, i) => opt.setAttribute("aria-selected", String(i === active)));
    input.setAttribute("aria-activedescendant", opts[active].id);
    opts[active].scrollIntoView({ block: "nearest" });
  }

  async function render() {
    const query = input.value.trim();
    const token = ++renderToken;
    if (!query) {
      close();
      return;
    }
    let catalog;
    try {
      catalog = await loadCatalog();
    } catch {
      return; // the shop page will show its own load error
    }
    const pricer = await createPricer();
    if (token !== renderToken) return; // a newer keystroke won

    const matches = rank(catalog, query);
    list.textContent = "";
    active = -1;
    input.removeAttribute("aria-activedescendant");

    matches.slice(0, MAX_RESULTS).forEach((product, i) => {
      const li = document.createElement("li");
      li.id = `search-option-${product.id}`;
      li.setAttribute("role", "option");
      li.setAttribute("aria-selected", "false");
      li.dataset.productId = String(product.id);

      const link = document.createElement("a");
      link.className = "search-result";
      link.href = `/shop/${product.id}`;
      link.tabIndex = -1; // focus stays in the input; arrows move the selection

      const category = CATEGORIES.find((c) => c.id === product.categoryId);
      const text = document.createElement("span");
      text.className = "search-result__text";
      const name = document.createElement("span");
      name.className = "search-result__name";
      name.textContent = product.name;
      const meta = document.createElement("span");
      meta.className = "search-result__meta";
      meta.textContent = category ? category.name : "";
      text.append(name, meta);

      const price = document.createElement("span");
      price.className = "search-result__price";
      price.textContent = pricer.formatPrice(product.price);

      link.append(createVisual(product, "search-result__media", { sizes: "56px" }), text, price);
      li.append(link);
      list.append(li);
    });

    empty.hidden = matches.length !== 0;
    empty.textContent = `No products match “${query}”.`;
    all.hidden = matches.length <= MAX_RESULTS;
    all.href = `/shop?q=${encodeURIComponent(query)}`;
    all.textContent = `View all ${matches.length} results`;
    open();
  }

  input.addEventListener("input", () => {
    clearTimeout(timer);
    timer = setTimeout(render, 90);
  });
  input.addEventListener("focus", () => {
    loadCatalog().catch(() => {});
    if (input.value.trim()) render();
  });

  input.addEventListener("keydown", (event) => {
    if (event.key === "ArrowDown" || event.key === "ArrowUp") {
      if (panel.hidden) {
        if (input.value.trim()) render();
        return;
      }
      event.preventDefault();
      setActive(active + (event.key === "ArrowDown" ? 1 : -1));
    } else if (event.key === "Enter" && !panel.hidden && active >= 0) {
      event.preventDefault();
      window.location.href = options()[active].querySelector("a").href;
    } else if (event.key === "Escape" && !panel.hidden) {
      event.preventDefault();
      event.stopPropagation(); // don't also close the mobile menu
      close();
    }
  });

  // Keep focus in the input while clicking a result, so the click lands.
  panel.addEventListener("pointerdown", (event) => event.preventDefault());
  form.addEventListener("focusout", (event) => {
    if (!form.contains(event.relatedTarget)) close();
  });
  form.addEventListener("submit", () => close());
}
