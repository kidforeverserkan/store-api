// Account dashboard (/orders): the signed-in user's details from
// GET /auth/me and their orders from GET /orders. Only real data — nothing
// here is invented or stored beyond what the API returns.
//
// Normal accounts get an "Edit profile" panel (#edit-profile) backed by the
// existing user endpoints. The shared public demo account (flagged by
// /auth/me) is shown as read-only instead. The API enforces that anyway;
// this just avoids offering edits it would reject.
import { apiFetch, ApiError, isLoggedIn, logout } from "/js/api.js";
import { formatAmount } from "/js/currency.js";
import { createVisual } from "/js/product-visuals.js";

const statusEl = document.getElementById("orders-status");
const signedOutEl = document.getElementById("account-signed-out");
const dashboardEl = document.getElementById("account-dashboard");
const listEl = document.getElementById("orders-list");
const emptyEl = document.getElementById("orders-empty");
const summaryEl = document.getElementById("orders-summary");

const STATUS_LABELS = { Paid: "Paid", PENDING: "Pending", FAILED: "Failed", CANCELED: "Canceled" };

function formatDate(iso) {
  try {
    return new Date(iso).toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" });
  } catch {
    return iso;
  }
}

function showSignedOut({ expired = false } = {}) {
  statusEl.hidden = true;
  dashboardEl.hidden = true;
  if (expired) {
    document.getElementById("account-signed-out-title").textContent = "Your session has expired";
    document.getElementById("account-signed-out-text").textContent = "Sign in again to see your account and orders.";
  }
  signedOutEl.hidden = false;
}

const ordersSection = document.getElementById("order-history");
const editSection = document.getElementById("account-edit");
const editButton = document.getElementById("edit-profile-button");

let currentUser = null;

function renderProfile(user) {
  currentUser = user;
  const name = user.name || user.email;
  document.getElementById("account-name").textContent = name;
  document.getElementById("account-email").textContent = user.email;
  document.getElementById("account-avatar").textContent = (name || "?").trim().charAt(0).toUpperCase();

  const demo = user.demoAccount === true;
  document.getElementById("account-card-title").textContent = demo ? "Public demo account" : "Account overview";
  document.getElementById("account-demo-note").hidden = !demo;
  editButton.hidden = demo;
}

// ----- edit profile (normal accounts only) -----------------------------------

function setMessage(el, text, kind) {
  el.textContent = text;
  el.classList.toggle("is-error", kind === "error");
  el.classList.toggle("is-success", kind === "success");
}

function firstFieldError(body) {
  if (body && typeof body === "object") {
    if (typeof body.error === "string") return body.error;
    const values = Object.values(body).filter((v) => typeof v === "string");
    if (values.length > 0) return values[0];
  }
  return null;
}

function canEdit() {
  return currentUser !== null && currentUser.demoAccount !== true;
}

// #edit-profile shows the edit panel; anything else shows the order history.
function applyView({ focus = false } = {}) {
  const editing = window.location.hash === "#edit-profile" && canEdit();
  editSection.hidden = !editing;
  ordersSection.hidden = editing;
  if (editing) {
    const form = document.getElementById("profile-form");
    form.name.value = currentUser.name || "";
    form.email.value = currentUser.email || "";
    if (focus) document.getElementById("account-edit-title").focus();
  }
}

async function saveProfile(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const message = document.getElementById("profile-message");
  const name = form.name.value.trim();
  const email = form.email.value.trim().toLowerCase();

  if (!name || !email) {
    setMessage(message, "Please enter both your name and email.", "error");
    return;
  }
  if (!form.email.checkValidity()) {
    setMessage(message, "Please enter a valid email address.", "error");
    return;
  }

  const button = form.querySelector('button[type="submit"]');
  button.disabled = true;
  setMessage(message, "Saving…");
  try {
    const updated = await apiFetch(`/users/${currentUser.id}`, {
      method: "PUT",
      body: JSON.stringify({ name, email }),
    });
    renderProfile({ ...currentUser, name: updated.name, email: updated.email });
    form.email.value = updated.email;
    setMessage(message, "Profile updated.", "success");
  } catch (err) {
    if (err instanceof ApiError && err.status === 409) {
      setMessage(message, "That email is already in use by another account.", "error");
    } else if (err instanceof ApiError && (err.status === 400 || err.status === 403)) {
      setMessage(message, firstFieldError(err.body) || "Please check your details and try again.", "error");
    } else {
      setMessage(message, "Couldn't save your changes. Please try again.", "error");
    }
  } finally {
    button.disabled = false;
  }
}

async function changePassword(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const message = document.getElementById("password-message");
  const oldPassword = form.currentPassword.value;
  const newPassword = form.newPassword.value;

  if (!oldPassword || !newPassword) {
    setMessage(message, "Please fill in your current and new password.", "error");
    return;
  }
  if (newPassword.length < 8 || newPassword.length > 25) {
    setMessage(message, "Your new password must be 8–25 characters long.", "error");
    return;
  }
  if (newPassword !== form.confirmPassword.value) {
    setMessage(message, "The new passwords don't match.", "error");
    return;
  }

  const button = form.querySelector('button[type="submit"]');
  button.disabled = true;
  setMessage(message, "Changing password…");
  try {
    await apiFetch(`/users/${currentUser.id}/change-password`, {
      method: "POST",
      body: JSON.stringify({ oldPassword, newPassword }),
      refreshOn401: false, // 401 here means "wrong current password"
    });
    form.reset();
    setMessage(message, "Password changed.", "success");
  } catch (err) {
    if (err instanceof ApiError && err.status === 401) {
      setMessage(message, "Your current password is incorrect.", "error");
    } else if (err instanceof ApiError && (err.status === 400 || err.status === 403)) {
      setMessage(message, firstFieldError(err.body) || "Please check your passwords and try again.", "error");
    } else {
      setMessage(message, "Couldn't change your password. Please try again.", "error");
    }
  } finally {
    button.disabled = false;
  }
}

function initDeleteAccount() {
  const start = document.getElementById("delete-account-button");
  const confirmBox = document.getElementById("delete-confirm");
  const confirmButton = document.getElementById("delete-account-confirm");
  const message = document.getElementById("delete-message");

  start.addEventListener("click", () => {
    setMessage(message, "");
    confirmBox.hidden = false;
    start.hidden = true;
    confirmButton.focus();
  });
  document.getElementById("delete-account-cancel").addEventListener("click", () => {
    confirmBox.hidden = true;
    start.hidden = false;
    start.focus();
  });
  confirmButton.addEventListener("click", async () => {
    confirmButton.disabled = true;
    try {
      await apiFetch(`/users/${currentUser.id}`, { method: "DELETE" });
      logout(); // clears the session and returns to the home page
    } catch (err) {
      confirmBox.hidden = true;
      start.hidden = false;
      if (err instanceof ApiError && err.status === 409) {
        setMessage(message, "This account has placed orders, so it can't be deleted.", "error");
      } else if (err instanceof ApiError && err.status === 403) {
        setMessage(message, firstFieldError(err.body) || "This account can't be deleted.", "error");
      } else {
        setMessage(message, "Couldn't delete your account. Please try again.", "error");
      }
    } finally {
      confirmButton.disabled = false;
    }
  });
}

document.getElementById("profile-form").addEventListener("submit", saveProfile);
document.getElementById("password-form").addEventListener("submit", changePassword);
initDeleteAccount();
window.addEventListener("hashchange", () => {
  if (currentUser) applyView({ focus: window.location.hash === "#edit-profile" });
});

// Each order is formatted in the currency it was charged in, from its
// recorded total — the header's currency selector doesn't apply here.
function renderOrders(orders) {
  listEl.textContent = "";
  for (const order of orders) {
    const li = document.createElement("li");
    li.className = "order-card";

    const header = document.createElement("div");
    header.className = "order-card__header";
    const title = document.createElement("span");
    title.className = "order-card__title";
    title.textContent = `Order #${order.id}`;
    const date = document.createElement("span");
    date.className = "order-card__date";
    date.textContent = formatDate(order.createdAt);
    const heading = document.createElement("span");
    heading.className = "order-card__heading";
    heading.append(title, date);
    const status = document.createElement("span");
    status.className = `order-card__status order-card__status--${String(order.status).toLowerCase()}`;
    status.textContent = STATUS_LABELS[order.status] || order.status;
    header.append(heading, status);

    const items = document.createElement("ul");
    items.className = "order-card__items";
    for (const item of order.items) {
      const itemLi = document.createElement("li");
      itemLi.className = "order-card__item";
      const label = document.createElement("span");
      label.textContent = `${item.quantity} × ${item.product.name}`;
      itemLi.append(createVisual(item.product, "order-card__thumb", { sizes: "40px" }), label);
      items.append(itemLi);
    }

    const total = document.createElement("p");
    total.className = "order-card__total";
    total.textContent = `Total: ${formatAmount(order.totalPrice, order.currency)}`;

    li.append(header, items, total);
    listEl.append(li);
  }
}

async function loadAccount() {
  if (!isLoggedIn()) {
    showSignedOut();
    return;
  }

  try {
    const [user, orders] = await Promise.all([apiFetch("/auth/me"), apiFetch("/orders")]);
    renderProfile(user);

    const list = orders || [];
    const paid = list.filter((o) => o.status === "Paid").length;
    document.getElementById("account-order-count").textContent = String(list.length);
    document.getElementById("account-paid-count").textContent = String(paid);
    summaryEl.textContent = list.length === 0 ? "" : `${list.length} ${list.length === 1 ? "order" : "orders"}`;

    if (list.length === 0) {
      emptyEl.hidden = false;
    } else {
      renderOrders(list);
      listEl.hidden = false;
    }
    statusEl.hidden = true;
    dashboardEl.hidden = false;
    applyView();
  } catch (err) {
    if (err instanceof ApiError && err.status === 401) {
      showSignedOut({ expired: true });
    } else {
      statusEl.textContent = "Couldn't load your account. Please try again shortly.";
    }
  }
}

loadAccount();
