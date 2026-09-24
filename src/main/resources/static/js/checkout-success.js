import { apiFetch, ApiError, isLoggedIn } from "/js/api.js";
import { updateCartBadge } from "/js/nav.js";
import { formatAmount } from "/js/currency.js";

const titleEl = document.getElementById("success-title");
const messageEl = document.getElementById("success-message");
const summaryEl = document.getElementById("order-summary");
const headingEl = document.getElementById("order-summary-heading");
const itemsEl = document.getElementById("order-summary-items");
const totalEl = document.getElementById("order-summary-total");

// A placed order is shown exactly as charged: its own recorded amounts in
// its own currency — never converted, and not affected by the header's
// currency selector.
function renderItems(order) {
  itemsEl.textContent = "";
  for (const item of order.items) {
    const li = document.createElement("li");
    li.textContent = `${item.quantity} × ${item.product.name} — ${formatAmount(item.price * item.quantity, order.currency)}`;
    itemsEl.append(li);
  }
  totalEl.textContent = formatAmount(order.totalPrice, order.currency);
}

async function loadOrderSummary() {
  const orderId = new URLSearchParams(window.location.search).get("orderId");
  if (!orderId) return;

  if (!isLoggedIn()) {
    messageEl.textContent = "Thank you — your order has been placed. Log in to view the full order details.";
    return;
  }

  try {
    const order = await apiFetch(`/orders/${orderId}`);

    // Anyone can open this URL (e.g. after a declined card + "back"), and
    // the order only becomes "Paid" once Stripe's webhook arrives — so the
    // page must not claim success until the order actually says so.
    if (order.status === "Paid") {
      titleEl.textContent = "Payment successful";
    } else if (order.status === "FAILED") {
      titleEl.textContent = "Payment failed";
      messageEl.textContent = "Your payment didn't go through, so this order won't be shipped.";
    } else {
      titleEl.textContent = "Order received";
      messageEl.textContent = "We're waiting for Stripe to confirm your payment. Your order history will show the status once it's confirmed.";
    }

    headingEl.textContent = `Order #${order.id}`;
    renderItems(order);

    summaryEl.hidden = false;
  } catch (err) {
    // Not fatal — the payment already succeeded on Stripe's side; we just
    // couldn't fetch the summary (e.g. token expired between redirect and
    // this fetch). The generic success message above still stands.
    if (!(err instanceof ApiError)) {
      messageEl.textContent = "Thank you — your order has been placed.";
    }
  }
}

// The cart was already cleared server-side once checkout started; drop the
// stale local reference so the badge and /cart page reflect that.
updateCartBadge();
loadOrderSummary();
