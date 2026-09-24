import { apiFetch, ApiError, clearCartId, getCartId, isLoggedIn } from "/js/api.js";
import { updateCartBadge } from "/js/nav.js";
import { createPricer, getCurrency, onCurrencyChange } from "/js/currency.js";
import { createVisual } from "/js/product-visuals.js";

const statusEl = document.getElementById("cart-status");
const contentEl = document.getElementById("cart-content");
const itemsEl = document.getElementById("cart-items");
const totalEl = document.getElementById("cart-total");
const currencyNoteEl = document.getElementById("currency-note");
const checkoutButton = document.getElementById("checkout-button");
const checkoutStatusEl = document.getElementById("checkout-status");

const cartId = getCartId();
let currentCart = null;

async function renderCart(cart) {
  currentCart = cart;
  itemsEl.textContent = "";

  if (!cart.items || cart.items.length === 0) {
    statusEl.textContent = "Your cart is empty.";
    contentEl.hidden = true;
    return;
  }

  const pricer = await createPricer();
  let totalMinor = 0;

  for (const item of cart.items) {
    // Line total = converted unit price × quantity — exactly how the
    // amounts sent to Stripe are built, so the totals always match.
    const lineMinor = pricer.line(item.product.price, item.quantity);
    totalMinor += lineMinor;

    const li = document.createElement("li");
    li.className = "cart-item";

    // Photo and name both open the product page. The photo link is skipped
    // by keyboard and screen readers, since the name link goes to the same place.
    const productUrl = `/shop/${item.product.id}`;
    const visualLink = document.createElement("a");
    visualLink.className = "cart-item__visual-link";
    visualLink.href = productUrl;
    visualLink.tabIndex = -1;
    visualLink.setAttribute("aria-hidden", "true");
    visualLink.append(createVisual(item.product, "cart-item__visual", { sizes: "64px" }));

    const name = document.createElement("a");
    name.className = "cart-item__name";
    name.href = productUrl;
    name.textContent = item.product.name;

    const price = document.createElement("span");
    price.className = "cart-item__price";
    price.textContent = pricer.formatPrice(item.product.price);

    const qty = document.createElement("input");
    qty.className = "cart-item__qty";
    qty.type = "number";
    qty.min = "1";
    qty.value = String(item.quantity);
    qty.setAttribute("aria-label", `Quantity for ${item.product.name}`);
    qty.addEventListener("change", () => updateQuantity(item.product.id, qty));

    const lineTotal = document.createElement("span");
    lineTotal.className = "cart-item__line-total";
    lineTotal.textContent = pricer.format(lineMinor);

    const removeButton = document.createElement("button");
    removeButton.className = "cart-item__remove";
    removeButton.type = "button";
    removeButton.textContent = "Remove";
    removeButton.addEventListener("click", () => removeItem(item.product.id));

    li.append(visualLink, name, price, qty, lineTotal, removeButton);
    itemsEl.append(li);
  }

  totalEl.textContent = pricer.format(totalMinor);

  // Be upfront that non-DKK prices are a fixed demo conversion.
  if (pricer.currency === "DKK") {
    currencyNoteEl.hidden = true;
  } else {
    currencyNoteEl.textContent = `Prices converted from DKK at a fixed demo rate (1 DKK = ${pricer.rate} ${pricer.currency}). You'll be charged in ${pricer.currency}.`;
    currencyNoteEl.hidden = false;
  }

  statusEl.hidden = true;
  contentEl.hidden = false;
}

async function loadCart() {
  if (!cartId) {
    statusEl.textContent = "Your cart is empty.";
    return;
  }

  try {
    const cart = await apiFetch(`/carts/${cartId}`);
    await renderCart(cart);
  } catch (err) {
    if (err instanceof ApiError && err.status === 404) {
      clearCartId(); // the saved cart no longer exists
      statusEl.textContent = "Your cart is empty.";
    } else {
      statusEl.textContent = "Couldn't load your cart. Please try again shortly.";
    }
  }
}

async function updateQuantity(productId, input) {
  const quantity = Math.max(1, Number(input.value) || 1);
  input.value = String(quantity);

  try {
    await apiFetch(`/carts/${cartId}/items/${productId}`, {
      method: "PUT",
      body: JSON.stringify({ quantity }),
    });
    await loadCart();
    await updateCartBadge();
  } catch {
    checkoutStatusEl.textContent = "Couldn't update quantity. Please try again.";
  }
}

async function removeItem(productId) {
  try {
    await apiFetch(`/carts/${cartId}/items/${productId}`, { method: "DELETE" });
    await loadCart();
    await updateCartBadge();
  } catch {
    checkoutStatusEl.textContent = "Couldn't remove that item. Please try again.";
  }
}

async function startCheckout() {
  if (!cartId) return;

  if (!isLoggedIn()) {
    window.location.href = "/login?next=/cart";
    return;
  }

  checkoutButton.disabled = true;
  checkoutStatusEl.textContent = "Starting checkout…";

  try {
    const result = await apiFetch("/checkout", {
      method: "POST",
      body: JSON.stringify({ cartId, currency: getCurrency() }),
    });
    // Full navigation to Stripe's hosted checkout page — not fetched via
    // apiFetch, this is a real page redirect off the site.
    window.location.href = result.checkoutUrl;
  } catch (err) {
    checkoutButton.disabled = false;
    if (err instanceof ApiError && err.status === 400) {
      checkoutStatusEl.textContent = "Your cart is empty.";
    } else {
      checkoutStatusEl.textContent = "Couldn't start checkout. Please try again.";
    }
  }
}

checkoutButton.addEventListener("click", startCheckout);
onCurrencyChange(() => {
  if (currentCart) renderCart(currentCart);
});
loadCart();
