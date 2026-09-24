import { apiFetch, ApiError, setToken } from "/js/api.js";

const form = document.getElementById("register-form");
const errorEl = document.getElementById("register-error");

function safeNext() {
  const next = new URLSearchParams(window.location.search).get("next");
  if (next && next.startsWith("/") && !next.startsWith("//")) {
    return next;
  }
  return "/";
}

function firstErrorMessage(body) {
  if (body && typeof body === "object") {
    const values = Object.values(body).filter((v) => typeof v === "string");
    if (values.length > 0) return values[0];
  }
  return "Couldn't create your account. Please check your details and try again.";
}

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  errorEl.hidden = true;

  const name = form.name.value;
  const email = form.email.value;
  const password = form.password.value;

  try {
    await apiFetch("/users", {
      method: "POST",
      body: JSON.stringify({ name, email, password }),
    });

    // Registration doesn't return a token, so log in immediately with the
    // same credentials for a one-step signup flow.
    const loginResult = await apiFetch("/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    });
    setToken(loginResult.token);
    window.location.href = safeNext();
  } catch (err) {
    if (err instanceof ApiError && (err.status === 400 || err.status === 401)) {
      errorEl.textContent = firstErrorMessage(err.body);
    } else {
      errorEl.textContent = "Couldn't create your account right now. Please try again.";
    }
    errorEl.hidden = false;
  }
});
