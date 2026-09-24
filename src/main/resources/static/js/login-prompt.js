// Shopping requires an account on the storefront: a signed-out visitor who
// tries to add something to the cart gets this dialog (log in / create
// account) instead of a guest cart. Browsing stays open to everyone.
//
// This is a storefront rule only. The cart API itself still supports guest
// carts by UUID; see the README's trade-offs.
import { isLoggedIn } from "/js/api.js";

let dialog = null;

function currentPath() {
  return window.location.pathname + window.location.search;
}

function buildDialog() {
  const el = document.createElement("dialog");
  el.className = "login-prompt";
  el.setAttribute("aria-labelledby", "login-prompt-title");
  el.setAttribute("aria-describedby", "login-prompt-text");
  el.innerHTML = `
    <form method="dialog" class="login-prompt__close-form">
      <button class="login-prompt__close" type="submit" aria-label="Close">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><path d="M6 6l12 12M18 6 6 18"/></svg>
      </button>
    </form>
    <svg class="login-prompt__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="8" r="3.6"/><path d="M4.8 20a7.2 7.2 0 0 1 14.4 0"/></svg>
    <h2 id="login-prompt-title" class="login-prompt__title">Log in to shop</h2>
    <p id="login-prompt-text" class="login-prompt__text">Please log in to add items to your cart.</p>
    <div class="login-prompt__actions">
      <a class="btn btn--primary" data-login-link href="/login">Log in</a>
      <a class="btn btn--outline" data-register-link href="/register">Create account</a>
    </div>`;
  // A click on the backdrop (outside the panel) closes it.
  el.addEventListener("click", (event) => {
    if (event.target === el) el.close();
  });
  document.body.append(el);
  return el;
}

function openPrompt() {
  dialog ||= buildDialog();
  const next = encodeURIComponent(currentPath());
  dialog.querySelector("[data-login-link]").href = `/login?next=${next}`;
  dialog.querySelector("[data-register-link]").href = `/register?next=${next}`;
  if (!dialog.open) dialog.showModal();
}

// True when the visitor may add to the cart. Otherwise shows the login
// prompt and returns false.
export function requireLoginToShop() {
  if (isLoggedIn()) return true;
  openPrompt();
  return false;
}
