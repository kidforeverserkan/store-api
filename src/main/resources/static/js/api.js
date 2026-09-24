// Shared API helper for the storefront. Talks to the existing REST API only
// — no new backend behaviour is assumed here beyond what's documented in the
// controllers.

const TOKEN_KEY = "accessToken";
const CART_KEY = "cartId";

// Access token lives in sessionStorage only: never the URL, never HTML,
// never logged, never localStorage. Tab-scoped by design.
export function getToken() {
  try {
    return sessionStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

export function setToken(token) {
  try {
    sessionStorage.setItem(TOKEN_KEY, token);
  } catch {
    // sessionStorage unavailable (private mode, etc.) — auth just won't
    // persist across a reload; not fatal for a single-page interaction.
  }
}

export function clearToken() {
  try {
    sessionStorage.removeItem(TOKEN_KEY);
  } catch {
    /* ignore */
  }
}

export function isLoggedIn() {
  return !!getToken();
}

// Cart id is not sensitive (an unguessable UUID with no ownership), so
// localStorage is fine — it's what lets the cart survive a reload/new tab.
export function getCartId() {
  try {
    return localStorage.getItem(CART_KEY);
  } catch {
    return null;
  }
}

export function setCartId(id) {
  try {
    localStorage.setItem(CART_KEY, id);
  } catch {
    /* ignore */
  }
}

export function clearCartId() {
  try {
    localStorage.removeItem(CART_KEY);
  } catch {
    /* ignore */
  }
}

export class ApiError extends Error {
  constructor(status, body) {
    super(`Request failed with ${status}`);
    this.status = status;
    this.body = body;
  }
}

async function doFetch(path, options) {
  const headers = new Headers(options.headers || {});
  headers.set("Accept", "application/json");
  if (options.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  const token = getToken();
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  return fetch(path, { ...options, headers });
}

// Fetch wrapper used by every page script. On a 401 it tries the refresh
// flow exactly once (the httpOnly refresh cookie is sent automatically for
// this same-origin request); if that succeeds it retries the original call
// with the new token, otherwise it clears the stale token and gives up.
// `refreshOn401: false` is for endpoints where 401 means "wrong credentials"
// (e.g. a wrong current password), not "session expired".
export async function apiFetch(path, { refreshOn401 = true, ...options } = {}) {
  let response = await doFetch(path, options);

  if (response.status === 401 && refreshOn401 && getToken()) {
    const refreshed = await tryRefresh();
    if (refreshed) {
      response = await doFetch(path, options);
    }
  }

  if (!response.ok) {
    let body = null;
    try {
      body = await response.json();
    } catch {
      /* no JSON body */
    }
    throw new ApiError(response.status, body);
  }

  if (response.status === 204) {
    return null;
  }

  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

async function tryRefresh() {
  try {
    const response = await fetch("/auth/refresh", {
      method: "POST",
      credentials: "same-origin",
    });
    if (!response.ok) {
      clearToken();
      return false;
    }
    const data = await response.json();
    setToken(data.token);
    return true;
  } catch {
    clearToken();
    return false;
  }
}

// Adds `quantity` units of a product to the visitor's cart, creating the
// (guest) cart on first use. Shared by the product page and every product
// card so all of them go through exactly the same existing API calls:
// POST adds one unit (or increments), then — only when more than one unit
// was asked for — PUT sets the item's new total.
//
// The cart id lives in localStorage, so it can outlive the cart itself (a
// database reset, a redeploy, a cleaned-up guest cart). If the server says
// that cart no longer exists (404), start a fresh cart and try once more —
// otherwise every "Add" would keep failing against a cart that's gone.
export async function addProductToCart(productId, quantity = 1) {
  const newCart = async () => {
    const cart = await apiFetch("/carts", { method: "POST" });
    setCartId(cart.id);
    return cart.id;
  };
  const addOne = (id) => apiFetch(`/carts/${id}/items`, {
    method: "POST",
    body: JSON.stringify({ productId: Number(productId) }),
  });

  let cartId = getCartId() || (await newCart());
  let item;
  try {
    item = await addOne(cartId);
  } catch (err) {
    if (!(err instanceof ApiError) || err.status !== 404 || err.body?.error !== "Cart not found.") throw err;
    clearCartId();
    cartId = await newCart();
    item = await addOne(cartId);
  }
  if (quantity > 1) {
    return apiFetch(`/carts/${cartId}/items/${productId}`, {
      method: "PUT",
      body: JSON.stringify({ quantity: item.quantity + quantity - 1 }),
    });
  }
  return item;
}

export function logout() {
  clearToken();
  window.location.href = "/";
}
