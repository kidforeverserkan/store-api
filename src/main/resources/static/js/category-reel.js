// Category reel: the home page's signature interaction. The five category
// cards move through by themselves — Electronics → Home & Kitchen → Books →
// Fitness → Office → Electronics … — with the active card in front and the
// rest receding. Only the Pause/Play button stops or restarts it.
//
// How it works
// - The track holds the five real cards followed by five inert, aria-hidden
//   clones. Advancing past Office slides onto the Electronics clone, then
//   the track jumps (invisibly) back to the real Electronics card, so the
//   reel always keeps flowing forward. Real cards never change DOM order,
//   which keeps keyboard Tab order stable and trap-free.
// - The active progress segment's CSS fill animation *is* the timer: when it
//   ends, the reel advances. Pausing pauses that animation, so the bar and
//   the countdown freeze together and resume exactly where they were.
// - Autoplay has one source of truth, `userPaused`, changed only by the
//   Pause/Play button. Hover, focus, touch, swipes and the arrows never pause
//   it; the arrows move one card and restart the countdown. Separately, the
//   timer is suspended while the tab is hidden or the reel is off-screen,
//   and picks up again on its own; that never changes the autoplay state.
// - With prefers-reduced-motion the reel never auto-advances: it becomes a
//   plain horizontally scrollable row (the viewport scrolls, never the page).

const REDUCED = window.matchMedia("(prefers-reduced-motion: reduce)");

export function initCategoryReel(root) {
  const viewport = root.querySelector(".category-reel__viewport");
  const track = root.querySelector(".category-reel__track");
  const segments = [...root.querySelectorAll(".reel-progress")];
  const scope = root.closest("section") || root;
  const prevBtn = scope.querySelector("[data-reel-prev]");
  const nextBtn = scope.querySelector("[data-reel-next]");
  const toggleBtn = scope.querySelector("[data-reel-toggle]");
  const status = scope.querySelector("[data-reel-status]");

  const real = [...track.children];
  const count = real.length;
  if (count === 0) return;

  // Clones for the seamless forward loop. Never focusable, never announced.
  for (const card of real) {
    const clone = card.cloneNode(true);
    clone.setAttribute("aria-hidden", "true");
    clone.setAttribute("inert", "");
    clone.classList.add("is-clone");
    clone.removeAttribute("id");
    track.append(clone);
  }
  const cards = [...track.children];

  let position = 0; // index into `cards` (0 … 2*count-1)
  let busy = false;
  // Arrow presses during a movement are queued (±2 at most) and played as
  // soon as it settles, instead of being dropped.
  let queued = 0;
  let userPaused = false;
  const suspended = new Set(); // "hidden" tab, "offscreen" reel

  const logical = (pos) => pos % count;
  const offsetOf = (pos) => cards[pos].offsetLeft - cards[0].offsetLeft;

  function setTransform(pos, animate) {
    track.style.transition = animate ? "" : "none";
    track.style.transform = `translate3d(${-offsetOf(pos)}px, 0, 0)`;
    if (!animate) void track.offsetWidth; // commit before re-enabling
  }

  function markActive() {
    const active = logical(position);
    cards.forEach((card, i) => card.classList.toggle("is-active", i === position));
    real.forEach((card, i) => {
      const link = card.querySelector("a");
      if (link) {
        if (i === active) link.setAttribute("aria-current", "true");
        else link.removeAttribute("aria-current");
      }
    });
    segments.forEach((seg, i) => {
      seg.classList.remove("is-active");
      seg.setAttribute("aria-current", i === active ? "step" : "false");
    });
    const seg = segments[active];
    if (seg) {
      void seg.offsetWidth; // restart the fill animation from zero
      seg.classList.add("is-active");
    }
    if (status) status.textContent = `${real[active].dataset.name}, ${active + 1} of ${count}`;
  }

  // Animate to a position; afterwards fold clone positions back onto the
  // matching real card without any visible jump.
  function goTo(target) {
    if (busy || target === position) return;
    busy = true;
    position = target;
    markActive();
    setTransform(position, true);

    let settled = false;
    const settle = () => {
      if (settled) return;
      settled = true;
      track.removeEventListener("transitionend", onEnd);
      if (position >= count) {
        position -= count;
        setTransform(position, false);
        markActive();
      }
      busy = false;
      if (queued !== 0) {
        const dir = Math.sign(queued);
        queued -= dir;
        (dir > 0 ? next : prev)();
      }
    };
    const onEnd = (event) => {
      if (event.target === track && event.propertyName === "transform") settle();
    };
    track.addEventListener("transitionend", onEnd);
    // Safety net if transitionend never arrives (e.g. element hidden).
    setTimeout(settle, 1400);
  }

  function next() {
    goTo(position + 1);
  }

  function prev() {
    if (busy) return;
    if (position === 0) {
      // Jump invisibly onto Electronics' clone, then slide back to Office.
      position = count;
      setTransform(position, false);
    }
    goTo(position - 1);
  }

  // Keyboard focus on a card brings that card to the front.
  function goToLogical(index) {
    goTo(index);
  }

  // ----- autoplay --------------------------------------------------------

  function update() {
    const running = !userPaused && suspended.size === 0 && !REDUCED.matches;
    root.classList.toggle("is-paused", !running);
    root.dataset.autoplay = userPaused || REDUCED.matches ? "off" : "on";
    if (toggleBtn) {
      toggleBtn.setAttribute("aria-pressed", String(userPaused));
      toggleBtn.setAttribute("aria-label", userPaused ? "Play category animation" : "Pause category animation");
      toggleBtn.classList.toggle("is-playing", !userPaused);
    }
    // Announce slide changes only while autoplay is off (the visitor drives),
    // never while it advances on its own.
    status?.setAttribute("aria-live", userPaused || REDUCED.matches ? "polite" : "off");
  }

  const suspend = (reason) => { suspended.add(reason); update(); };
  const unsuspend = (reason) => { suspended.delete(reason); update(); };

  // The timer: the active segment's fill animation ending means "advance".
  root.addEventListener("animationend", (event) => {
    if (event.animationName === "reel-fill" && !REDUCED.matches) next();
  });

  // Keyboard focus on a card brings that card to the front (navigation only;
  // autoplay keeps its state).
  scope.addEventListener("focusin", (event) => {
    const card = event.target.closest(".reel-card");
    const index = real.indexOf(card);
    if (index >= 0 && index !== logical(position)) goToLogical(index);
  });

  // Touch: swipe to move one card. Touching never pauses autoplay.
  let touchStartX = null;
  viewport.addEventListener("pointerdown", (e) => {
    if (e.pointerType !== "touch") return;
    touchStartX = e.clientX;
  });
  const endTouch = (e) => {
    if (e.pointerType !== "touch") return;
    if (touchStartX !== null && !REDUCED.matches) {
      const dx = e.clientX - touchStartX;
      if (Math.abs(dx) > 40) (dx < 0 ? next : prev)();
    }
    touchStartX = null;
  };
  viewport.addEventListener("pointerup", endTouch);
  viewport.addEventListener("pointercancel", endTouch);

  document.addEventListener("visibilitychange", () => {
    if (document.hidden) suspend("hidden");
    else unsuspend("hidden");
  });

  if ("IntersectionObserver" in window) {
    new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting) unsuspend("offscreen");
      else suspend("offscreen");
    }, { threshold: 0.35 }).observe(root);
  }

  toggleBtn?.addEventListener("click", () => {
    userPaused = !userPaused;
    update();
  });

  // Arrows are secondary controls; in the static (reduced-motion) layout
  // they scroll the row by one card instead.
  const step = (dir) => {
    if (REDUCED.matches) {
      viewport.scrollBy({ left: dir * (real[0].getBoundingClientRect().width + 16), behavior: "auto" });
    } else if (busy) {
      queued = Math.max(-2, Math.min(2, queued + dir));
    } else {
      (dir > 0 ? next : prev)();
    }
  };
  prevBtn?.addEventListener("click", () => step(-1));
  nextBtn?.addEventListener("click", () => step(1));

  // Keep the active card aligned when the layout width changes.
  window.addEventListener("resize", () => {
    if (!REDUCED.matches) setTransform(position, false);
  });

  function applyMotionPreference() {
    root.classList.toggle("is-static", REDUCED.matches);
    // Nothing moves on its own in the static layout, so pause/play is moot.
    if (toggleBtn) toggleBtn.hidden = REDUCED.matches;
    if (REDUCED.matches) {
      track.style.transform = "";
      track.style.transition = "none";
    } else {
      viewport.scrollLeft = 0;
      setTransform(position, false);
    }
    update();
  }
  REDUCED.addEventListener("change", applyMotionPreference);

  applyMotionPreference();
  markActive();
  root.classList.add("is-ready");
}
