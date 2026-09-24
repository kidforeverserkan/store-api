// Pointer-driven depth for large product photos (hero, product detail):
// the image leans a few pixels/degrees toward the pointer. Only for a fine
// pointer (mouse/trackpad) and only when the visitor allows motion; it
// writes two CSS custom properties and lets CSS do the rendering.
const REDUCED = window.matchMedia("(prefers-reduced-motion: reduce)");
const FINE = window.matchMedia("(pointer: fine)");

export function attachPointerDepth(area, target = area) {
  if (!area || !target) return;
  let frame = 0;
  let x = 0;
  let y = 0;

  const apply = () => {
    frame = 0;
    target.style.setProperty("--depth-x", x.toFixed(3));
    target.style.setProperty("--depth-y", y.toFixed(3));
  };

  area.addEventListener("pointermove", (event) => {
    if (REDUCED.matches || !FINE.matches || event.pointerType !== "mouse") return;
    const rect = area.getBoundingClientRect();
    x = ((event.clientX - rect.left) / rect.width - 0.5) * 2; // -1 … 1
    y = ((event.clientY - rect.top) / rect.height - 0.5) * 2;
    if (!frame) frame = requestAnimationFrame(apply);
  });
  area.addEventListener("pointerleave", () => {
    x = 0;
    y = 0;
    if (!frame) frame = requestAnimationFrame(apply);
  });
}
