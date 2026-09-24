import { apiFetch, ApiError, setToken } from "/js/api.js";

const form = document.getElementById("login-form");
const errorEl = document.getElementById("login-error");

// Only ever redirect to a relative, same-site path. A raw `next` value could
// otherwise be used for an open redirect (e.g. ?next=https://evil.example).
function safeNext() {
  const next = new URLSearchParams(window.location.search).get("next");
  if (next && next.startsWith("/") && !next.startsWith("//")) {
    return next;
  }
  return "/";
}

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  errorEl.hidden = true;

  const email = form.email.value;
  const password = form.password.value;

  try {
    const result = await apiFetch("/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    });
    setToken(result.token);
    window.location.href = safeNext();
  } catch (err) {
    if (err instanceof ApiError && err.status === 401) {
      errorEl.textContent = "Invalid email or password.";
    } else if (err instanceof ApiError && err.status === 400) {
      errorEl.textContent = "Please enter a valid email and password.";
    } else {
      errorEl.textContent = "Couldn't log in right now. Please try again.";
    }
    errorEl.hidden = false;
  }
});
