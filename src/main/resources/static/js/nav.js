// Runs on every page (included by the header fragment): keeps the cart
// badge, the account link, the mobile menu and the search box in sync with
// actual client state.
import { apiFetch, ApiError, clearCartId, getCartId, isLoggedIn, logout } from "/js/api.js";
import { getCurrency, setCurrency } from "/js/currency.js";
import { initSearchPanel } from "/js/search.js";

// The badge gives a small pulse when the count goes up (an item was just
// added) — not on page load, and not when items are removed.
let lastBadgeCount = null;

function bump(badge) {
  badge.classList.remove("cart-badge--bump");
  void badge.offsetWidth; // restart the animation if it's mid-flight
  badge.classList.add("cart-badge--bump");
}

export async function updateCartBadge() {
  const badge = document.getElementById("cart-count");
  if (!badge) return;

  const cartId = getCartId();
  if (!cartId) {
    badge.hidden = true;
    lastBadgeCount = 0;
    return;
  }

  try {
    const cart = await apiFetch(`/carts/${cartId}`);
    const count = (cart.items || []).reduce((sum, item) => sum + item.quantity, 0);
    if (count > 0) {
      const grew = lastBadgeCount !== null && count > lastBadgeCount;
      badge.textContent = String(count);
      badge.hidden = false;
      if (grew) bump(badge);
    } else {
      badge.hidden = true;
    }
    lastBadgeCount = count;
  } catch (err) {
    // Cart may have been cleared server-side or not exist any more — either
    // way, no badge to show. A cart that's gone (404) is forgotten so the
    // next "Add" starts a fresh one instead of polling a dead id.
    badge.hidden = true;
    if (err instanceof ApiError && err.status === 404) {
      clearCartId();
      lastBadgeCount = 0;
    }
  }
}

// Polite screen-reader announcements for actions that don't move focus
// (e.g. "Added to cart" from a product card).
export function announce(message) {
  const region = document.getElementById("site-announcer");
  if (!region) return;
  region.textContent = "";
  // A fresh text node after a tick makes repeated messages re-announce.
  setTimeout(() => { region.textContent = message; }, 30);
}

// Header account control: "Sign in" for guests, "Account" (order history)
// once logged in. Any element with [data-logout] logs out.
function updateAccountLink() {
  const link = document.getElementById("account-link");
  const label = document.getElementById("account-label");
  if (link && label) {
    const loggedIn = isLoggedIn();
    link.href = loggedIn ? "/orders" : "/login";
    label.textContent = loggedIn ? "Account" : "Sign in";
  }
  for (const button of document.querySelectorAll("[data-logout]")) {
    button.hidden = !isLoggedIn();
    button.addEventListener("click", (event) => {
      event.preventDefault();
      logout();
    });
  }
}

// Mobile navigation: below the desktop breakpoint the nav links, search and
// service status live in a panel behind the menu button.
function initMobileMenu() {
  const header = document.querySelector(".site-header");
  const toggle = document.getElementById("menu-toggle");
  const panel = document.getElementById("site-menu");
  if (!header || !toggle || !panel) return;

  const setOpen = (open, { restoreFocus = false } = {}) => {
    header.classList.toggle("is-menu-open", open);
    toggle.setAttribute("aria-expanded", String(open));
    toggle.setAttribute("aria-label", open ? "Close menu" : "Open menu");
    if (!open && restoreFocus) toggle.focus();
  };

  toggle.addEventListener("click", () => setOpen(toggle.getAttribute("aria-expanded") !== "true"));
  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && header.classList.contains("is-menu-open")) setOpen(false, { restoreFocus: true });
  });
  panel.addEventListener("click", (event) => {
    if (event.target.closest("a")) setOpen(false);
  });
  // Matches the CSS breakpoint where the bar collapses into the menu panel.
  window.matchMedia("(min-width: 1101px)").addEventListener("change", (event) => {
    if (event.matches) setOpen(false);
  });
}

// The header search submits to /shop?q=… from any page; on the shop page
// itself shop.js filters live as you type.
function initSearch() {
  const input = document.getElementById("site-search");
  if (!input) return;
  if (window.location.pathname === "/shop") {
    input.value = new URLSearchParams(window.location.search).get("q") || "";
  }
  initSearchPanel();
}

// Header currency selector: a button that opens a two-item menu. Keeps the
// label/checkmark in sync with the stored choice and supports mouse, touch
// and keyboard (Enter/Space/ArrowDown to open, arrows to move, Escape or a
// click elsewhere to close).
function initCurrencySwitcher() {
  const button = document.getElementById("currency-button");
  const menu = document.getElementById("currency-menu");
  if (!button || !menu) return;

  const current = document.getElementById("currency-current");
  const options = [...menu.querySelectorAll("[data-currency]")];

  function render() {
    const code = getCurrency();
    current.textContent = code;
    button.setAttribute("aria-label", `Currency: ${code}`);
    for (const option of options) {
      option.setAttribute("aria-checked", String(option.dataset.currency === code));
    }
  }

  function open() {
    menu.hidden = false;
    button.setAttribute("aria-expanded", "true");
    (options.find((o) => o.getAttribute("aria-checked") === "true") || options[0]).focus();
  }

  function close({ restoreFocus = false } = {}) {
    menu.hidden = true;
    button.setAttribute("aria-expanded", "false");
    if (restoreFocus) button.focus();
  }

  button.addEventListener("click", () => (menu.hidden ? open() : close()));
  button.addEventListener("keydown", (event) => {
    if (event.key === "ArrowDown" && menu.hidden) {
      event.preventDefault();
      open();
    }
  });

  menu.addEventListener("keydown", (event) => {
    const index = options.indexOf(document.activeElement);
    if (event.key === "ArrowDown" || event.key === "ArrowUp") {
      event.preventDefault();
      const step = event.key === "ArrowDown" ? 1 : -1;
      options[(index + step + options.length) % options.length].focus();
    } else if (event.key === "Escape") {
      close({ restoreFocus: true });
    } else if (event.key === "Tab") {
      close();
    }
  });

  for (const option of options) {
    option.addEventListener("click", () => {
      setCurrency(option.dataset.currency);
      close({ restoreFocus: true });
    });
  }

  document.addEventListener("click", (event) => {
    if (!menu.hidden && !button.parentElement.contains(event.target)) close();
  });

  window.addEventListener("currencychange", render);
  render();
}

// Header "Server online/offline" pill, driven by the real Actuator health
// endpoint (which exposes only {"status": "..."} — no details). One check on
// load, then one every HEALTH_INTERVAL_MS. The next check is only scheduled
// after the current one finishes, so checks never overlap, and checks are
// skipped while the tab is hidden (re-checked as soon as it's visible again).
const HEALTH_INTERVAL_MS = 30_000;
const HEALTH_TIMEOUT_MS = 5_000;

function initServerStatus() {
  const pill = document.getElementById("server-status");
  const text = document.getElementById("server-status-text");
  if (!pill || !text) return;

  let timer = null;

  function render(online) {
    pill.classList.remove("status-pill--checking");
    pill.classList.toggle("status-pill--offline", !online);
    text.textContent = online ? "Server online" : "Server offline";
  }

  async function isServerUp() {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), HEALTH_TIMEOUT_MS);
    try {
      const response = await fetch("/actuator/health", {
        headers: { Accept: "application/json" },
        cache: "no-store",
        signal: controller.signal,
      });
      if (!response.ok) return false;
      const body = await response.json();
      return body?.status === "UP";
    } catch {
      // Network failure, timeout (abort) or a non-JSON body: all "offline".
      return false;
    } finally {
      clearTimeout(timeout);
    }
  }

  let inFlight = false;

  async function check() {
    if (inFlight) return; // a check is already running; it will reschedule
    clearTimeout(timer);
    inFlight = true;
    try {
      if (!document.hidden) {
        render(await isServerUp());
      }
    } finally {
      inFlight = false;
      timer = setTimeout(check, HEALTH_INTERVAL_MS);
    }
  }

  document.addEventListener("visibilitychange", () => {
    if (!document.hidden) check();
  });

  check();
}

updateCartBadge();
updateAccountLink();
initMobileMenu();
initSearch();
initCurrencySwitcher();
initServerStatus();
