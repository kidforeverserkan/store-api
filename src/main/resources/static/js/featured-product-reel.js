// Hero featured-product reel: the home hero moves through the photographed
// products by itself — Headset → Coffee Maker → Book → Dumbbells → Lamp →
// Headset … — in the same spirit as the category reel below it (same timing
// model and pause rules, its own component).
//
// - Every value shown (name, category, description, price, link) comes from
//   the API product; the photo comes from the product-image mapping.
// - Slides are the stacked hero photos: the incoming one glides in from the
//   right as the outgoing one drifts left and fades (CSS transitions on
//   .is-active / .is-leaving); the text panel dissolves and resolves in step.
// - The active progress segment's fill animation is the timer; pausing
//   pauses it.
// - Autoplay has one source of truth, `userPaused`, and only the Pause/Play
//   button changes it. Hover, focus, touch and "Next" never pause it; "Next"
//   just moves on and restarts the countdown. Separately, the timer is
//   suspended while the tab is hidden or the hero is scrolled off-screen, and
//   picks up again on its own; that never changes the autoplay state.
// - prefers-reduced-motion: never auto-advances; "Next" still steps through.
import { categoryById, createProductImg } from "/js/product-visuals.js";

const REDUCED = window.matchMedia("(prefers-reduced-motion: reduce)");
const TEXT_SWAP_MS = 320;

export function initFeaturedReel({ hero, products, pricer }) {
  const media = hero.querySelector(".hero__media");
  const aside = hero.querySelector(".featured-reel");
  const textEl = aside.querySelector("[data-feature-text]");
  const progress = aside.querySelector("[data-feature-progress]");
  const toggleBtn = aside.querySelector("[data-feature-toggle]");
  const nextBtn = aside.querySelector("[data-feature-next]");
  const status = aside.querySelector("[data-feature-status]");
  const chip = aside.querySelector("#hero-product");

  // One slide per product, reusing the photo already in the page for the
  // first one (it's the preloaded LCP image).
  const slides = products.map((product) => {
    let img = media.querySelector(`.hero__img[data-product-id="${product.id}"]`);
    if (!img) {
      img = createProductImg(product, { sizes: "(max-width: 900px) 100vw, 60vw" });
      img.className = "hero__img";
      img.dataset.productId = String(product.id);
      media.append(img);
    }
    return { product, img };
  });
  if (slides.length === 0) return null;

  let index = 0;
  let userPaused = false;
  const suspended = new Set(); // "hidden" tab, "offscreen" hero

  slides.forEach(() => {
    const seg = document.createElement("span");
    seg.className = "feature-progress";
    seg.innerHTML = '<span class="feature-progress__fill"></span>';
    progress.append(seg);
  });
  const segments = [...progress.children];

  function renderText(product) {
    const category = categoryById(product.categoryId);
    aside.querySelector("[data-feature-icon]").textContent = category ? category.emoji : "";
    aside.querySelector("[data-feature-category]").textContent = category ? category.name : "";
    aside.querySelector("[data-feature-name]").textContent = product.name;
    aside.querySelector("[data-feature-desc]").textContent = product.description;
    chip.href = `/shop/${product.id}`;
    chip.querySelector("[data-name]").textContent = product.name;
    chip.setAttribute("aria-label", `View ${product.name}`);
    renderPrice(product);
  }

  function renderPrice(product) {
    chip.querySelector("[data-price]").textContent = pricer.formatPrice(product.price);
  }

  function markProgress() {
    segments.forEach((seg) => seg.classList.remove("is-active"));
    const seg = segments[index];
    void seg.offsetWidth; // restart the fill from zero
    seg.classList.add("is-active");
    status.textContent = `${slides[index].product.name}, ${index + 1} of ${slides.length}`;
  }

  function show(nextIndex) {
    if (nextIndex === index) return;
    const from = slides[index];
    const to = slides[nextIndex];
    index = nextIndex;

    from.img.classList.remove("is-active");
    from.img.classList.add("is-leaving");
    setTimeout(() => from.img.classList.remove("is-leaving"), 1600);
    to.img.classList.remove("is-leaving");
    to.img.classList.add("is-active");

    textEl.classList.add("is-swapping");
    setTimeout(() => {
      renderText(to.product);
      textEl.classList.remove("is-swapping");
    }, REDUCED.matches ? 0 : TEXT_SWAP_MS);

    markProgress();
  }

  const next = () => show((index + 1) % slides.length);

  // ----- autoplay ----------------------------------------------------------

  function update() {
    const running = !userPaused && suspended.size === 0 && !REDUCED.matches;
    aside.classList.toggle("is-paused", !running);
    aside.dataset.autoplay = userPaused || REDUCED.matches ? "off" : "on";
    toggleBtn.setAttribute("aria-pressed", String(userPaused));
    toggleBtn.setAttribute("aria-label", userPaused ? "Play featured products" : "Pause featured products");
    toggleBtn.classList.toggle("is-playing", !userPaused);
    toggleBtn.hidden = REDUCED.matches;
    // Announce slide changes only while autoplay is off (the visitor drives).
    status.setAttribute("aria-live", userPaused || REDUCED.matches ? "polite" : "off");
  }
  const suspend = (reason) => { suspended.add(reason); update(); };
  const unsuspend = (reason) => { suspended.delete(reason); update(); };

  aside.addEventListener("animationend", (event) => {
    if (event.animationName === "feature-fill" && !REDUCED.matches) next();
  });

  document.addEventListener("visibilitychange", () => (document.hidden ? suspend("hidden") : unsuspend("hidden")));
  if ("IntersectionObserver" in window) {
    new IntersectionObserver(([entry]) => (entry.isIntersecting ? unsuspend("offscreen") : suspend("offscreen")), { threshold: 0.3 }).observe(hero);
  }

  toggleBtn.addEventListener("click", () => {
    userPaused = !userPaused;
    update();
  });
  nextBtn.addEventListener("click", next);
  REDUCED.addEventListener("change", update);

  renderText(slides[0].product);
  markProgress();
  update();
  aside.hidden = false;
  if (slides.length < 2) {
    progress.hidden = true;
    toggleBtn.hidden = true;
    nextBtn.hidden = true;
  }

  return {
    // A currency change re-prices the visible product in place.
    reprice(newPricer) {
      pricer = newPricer;
      renderPrice(slides[index].product);
    },
  };
}
