// Behaviour tests for the storefront redesign: what the visitor can see and
// do, not pixel snapshots. Product data always comes from the running API.
const { test: base, expect } = require("@playwright/test");

// A signed-in shopper for tests that add to the cart or manage an account.
// Each test registers its own QA user and deletes it again afterwards, so
// runs don't leave accounts behind. Guest carts can't be deleted through the
// API, so those remain in the local database.
const SHOPPER_PASSWORD = "E2e-Test-Pass-123";

async function createShopper(request) {
  const email = `qa-e2e-${Date.now()}-${Math.floor(Math.random() * 1e6)}@example.com`;
  const created = await request.post("/users", { data: { name: "E2E Shopper", email, password: SHOPPER_PASSWORD } });
  expect(created.ok(), await created.text()).toBe(true);
  const { id } = await created.json();
  const login = await request.post("/auth/login", { data: { email, password: SHOPPER_PASSWORD } });
  expect(login.ok()).toBe(true);
  const { token } = await login.json();
  return { id, email, password: SHOPPER_PASSWORD, token };
}

const test = base.extend({
  shopper: async ({ request }, use) => {
    const shopper = await createShopper(request);
    await use(shopper);
    // A test may already have deleted the account itself.
    const headers = { Authorization: `Bearer ${shopper.token}` };
    if ((await request.get("/auth/me", { headers })).ok()) {
      await request.delete(`/users/${shopper.id}`, { headers });
    }
  },
});

// Signs the page in the same way the login page does (access token in
// sessionStorage), on every navigation.
async function signIn(page, shopper) {
  await page.addInitScript((token) => sessionStorage.setItem("accessToken", token), shopper.token);
}

const BOOK_IDS = [31, 32, 33, 34, 35, 36, 37, 38, 39, 40];
// Every product has a photo; the emoji fallback is exercised separately.
const PHOTO_PRODUCT_IDS = Array.from({ length: 58 }, (_, i) => i + 1);
const CATEGORY_ORDER = ["Electronics", "Home & Kitchen", "Books", "Fitness", "Office"];
// Reel timing: 4.8 s per category + 1.1 s movement, plus slack.
const REEL_STEP_MS = 8_000;

// Fail any test that logs an application error to the console. (The
// offline, stale-cart and password tests provoke a 503 / 404 / 401, which
// the browser itself logs.)
let appErrors = [];
test.beforeEach(async ({ page }, testInfo) => {
  appErrors = [];
  page.on("pageerror", (e) => appErrors.push(e.message));
  page.on("console", (m) => {
    if (m.type() === "error" && !/offline|stale|password/.test(testInfo.title)) appErrors.push(m.text());
  });
});
test.afterEach(async () => {
  expect(appErrors, "console / page errors").toEqual([]);
});

// Take focus and the pointer off the reel without scrolling the page (a
// scroll would move the reel out of view, which suspends its timer).
async function leaveReel(page) {
  await page.evaluate(() => document.activeElement?.blur());
  await page.mouse.move(8, 8); // over the sticky header
}

// Mid-loop the active card can briefly be Electronics' clone; it shows the
// same name, so reading whichever card is active is correct.
const activeCategory = (page) => page.locator(".reel-card.is-active .reel-card__name").first().textContent();

// ---------------------------------------------------------------- home

test("homepage loads with the redesigned hero, header and live service status", async ({ page }) => {
  await page.goto("/");
  await expect(page).toHaveTitle(/Store API/);
  await expect(page.getByRole("heading", { level: 1 })).toHaveText("A Better Everyday");
  await expect(page.getByRole("link", { name: "Shop now" })).toHaveAttribute("href", "/shop");
  expect(await page.locator(".hero__img.is-active").getAttribute("loading"), "hero image is not lazy").not.toBe("lazy");
  await expect(page.locator("#server-status-text")).toHaveText("Server online");
  for (const name of ["Shop", "Categories", "About", "Contact"]) {
    await expect(page.getByRole("navigation", { name: "Main" }).getByRole("link", { name })).toBeVisible();
  }
  await expect(page.locator("#featured-grid .product-card")).toHaveCount(5);
});

test("the header's Categories link opens the shop, where categories are chosen", async ({ page }) => {
  await page.goto("/");
  const link = page.getByRole("navigation", { name: "Main" }).getByRole("link", { name: "Categories" });
  await expect(link).toHaveAttribute("href", "/shop");
  await link.click();
  await expect(page).toHaveURL(/\/shop$/);
  await expect(page.locator(".category-chip", { hasText: "Books" })).toBeVisible();
  await expect(page.locator("#product-grid .product-card")).toHaveCount(58);

  // The home page's category cards still go straight to their category.
  await page.goto("/");
  await expect(page.locator('.reel-card:not(.is-clone) a[href="/shop?category=books"]')).toHaveCount(1);
});

test("about and contact are real pages with the shared layout", async ({ page }) => {
  for (const [path, heading] of [["/about", "About Store API"], ["/contact", "Contact"]]) {
    await page.goto(path);
    await expect(page.getByRole("heading", { level: 1 })).toHaveText(heading);
    await expect(page.locator(".site-header")).toBeVisible();
    await expect(page.locator(".site-footer")).toBeVisible();
  }
});

// ---------------------------------------------------- product imagery

test("real photos appear only on the mapped products; the rest keep their emoji", async ({ page, request }) => {
  const products = await (await request.get("/products")).json();
  expect(products).toHaveLength(58);

  await page.goto("/shop");
  const cards = page.locator("#product-grid .product-card");
  await expect(cards).toHaveCount(58);

  const rendered = await cards.evaluateAll((els) => els.map((el) => ({
    id: Number(el.dataset.productId),
    name: el.querySelector(".product-card__name").textContent,
    photo: el.querySelector(".product-card__media img")?.getAttribute("src") || null,
    emoji: el.querySelector(".product-card__media-emoji")?.textContent || null,
    mediaHidden: el.querySelector(".product-card__media").getAttribute("aria-hidden"),
  })));

  for (const card of rendered) {
    const api = products.find((p) => p.id === card.id);
    expect(card.name, "name comes from the API").toBe(api.name);
    expect(card.mediaHidden).toBe("true");
    if (PHOTO_PRODUCT_IDS.includes(card.id)) {
      expect(card.photo, `product ${card.id} has its photo`).toMatch(new RegExp(`/images/products/product-${card.id}-.*\.webp$`));
      expect(card.emoji).toBeNull();
    } else {
      expect(card.photo, `product ${card.id} has no photo`).toBeNull();
      expect(card.emoji, `product ${card.id} keeps an emoji`).toBeTruthy();
    }
  }
  // Names never carry emoji — the visual is presentation only.
  expect(products.every((p) => !/\p{Extended_Pictographic}/u.test(p.name))).toBe(true);
});

test("every book shows its own cover on its card, detail page and in the cart", async ({ page, request, shopper }) => {
  await signIn(page, shopper);
  const products = await (await request.get("/products")).json();
  await page.goto("/shop?category=books");
  const cards = page.locator("#product-grid .product-card");
  await expect(cards).toHaveCount(BOOK_IDS.length);
  for (const id of BOOK_IDS) {
    const img = page.locator(`.product-card[data-product-id="${id}"] .product-card__media img`);
    await img.scrollIntoViewIfNeeded();
    await expect(img).toHaveAttribute("src", new RegExp(`/images/products/product-${id}-[a-z0-9-]+-\\d+\\.webp$`));
    await expect.poll(() => img.evaluate((el) => el.complete && el.naturalWidth > 0), `cover ${id} loads`).toBe(true);
    await expect(page.locator(`.product-card[data-product-id="${id}"] .product-card__name`)).toHaveText(products.find((p) => p.id === id).name);
  }

  await page.goto("/shop/37");
  const detail = page.locator("#product-visual img");
  await expect(detail).toHaveAttribute("src", /product-37-cpp-primer-960\.webp$/);
  expect(await detail.evaluate((el) => el.complete && el.naturalWidth > 0)).toBe(true);
  await page.locator("#add-to-cart").click();
  await expect(page.locator("#cart-count")).toHaveText("1");

  await page.goto("/cart");
  await expect(page.locator(".cart-item", { hasText: "C++ Primer" }).locator("img")).toHaveAttribute("src", /product-37-/);
});

// ------------------------------------------------ hero featured reel

const FEATURED_ORDER = [
  { id: 10, name: "Noise Cancelling Headset", category: "Electronics" },
  { id: 22, name: "Programmable Coffee Maker", category: "Home & Kitchen" },
  { id: 38, name: "The Pragmatic Programmer", category: "Books" },
  { id: 44, name: "Adjustable Dumbbell Set", category: "Fitness" },
  { id: 51, name: "LED Desk Lamp", category: "Office" },
];
const FEATURED_STEP_MS = 9_000; // 6 s per product + transition + slack

// Everything the hero shows for the current product, read together.
const heroState = (page) => page.evaluate(() => ({
  name: document.querySelector("[data-feature-name]").textContent,
  category: document.querySelector("[data-feature-category]").textContent,
  desc: document.querySelector("[data-feature-desc]").textContent,
  price: document.querySelector("#hero-product [data-price]").textContent.replace(/\u00a0/g, " "),
  href: document.querySelector("#hero-product").getAttribute("href"),
  image: document.querySelector(".hero__img.is-active")?.dataset.productId,
}));

async function leaveHero(page) {
  await page.evaluate(() => document.activeElement?.blur());
  await page.mouse.move(8, 8); // over the header, outside the hero
}

test("hero featured product changes by itself, with image and details in step", async ({ page, request }) => {
  const products = await (await request.get("/products")).json();
  await page.goto("/");
  await leaveHero(page);
  await expect.poll(async () => (await heroState(page)).name).toBe(FEATURED_ORDER[0].name);

  for (const expected of FEATURED_ORDER.slice(0, 3)) {
    await expect.poll(async () => (await heroState(page)).name, { timeout: FEATURED_STEP_MS }).toBe(expected.name);
    await page.waitForTimeout(400); // let the text swap settle
    const state = await heroState(page);
    const api = products.find((p) => p.id === expected.id);
    expect(state.image, "photo matches the product").toBe(String(expected.id));
    expect(state.category).toBe(expected.category);
    expect(state.desc, "description comes from the API").toBe(api.description);
    expect(state.price).toBe(new Intl.NumberFormat("da-DK", { style: "currency", currency: "DKK" }).format(api.price).replace(/\u00a0/g, " "));
    expect(state.href).toBe(`/shop/${expected.id}`);
  }
});

test("hero featured reel loops from the lamp back to the headset", async ({ page }) => {
  test.setTimeout(60_000);
  await page.goto("/");
  const next = page.getByRole("button", { name: "Next featured product" });
  for (let i = 0; i < 4; i++) await next.click();
  await expect.poll(async () => (await heroState(page)).name).toBe("LED Desk Lamp");
  await leaveHero(page);
  await expect.poll(async () => (await heroState(page)).name, { timeout: FEATURED_STEP_MS }).toBe("Noise Cancelling Headset");
  await expect.poll(async () => (await heroState(page)).image).toBe("10");
});

// Only the Pause/Play button changes autoplay. The pointer and focus are
// deliberately left on the hero and its buttons throughout, which is what used
// to freeze it (hover and focus were pause reasons).
test("hero featured reel: hover and Next never pause it; Pause stops and Play resumes it", async ({ page }) => {
  test.setTimeout(90_000);
  const heroName = async () => (await heroState(page)).name;
  await page.goto("/");
  const aside = page.locator(".featured-reel");
  const toggle = page.locator("[data-feature-toggle]");
  await expect(aside).toBeVisible();
  await expect(aside).toHaveAttribute("data-autoplay", "on");

  // Hovering the photo: autoplay keeps running.
  const media = await page.locator(".hero__media").boundingBox();
  await page.mouse.move(media.x + media.width * 0.75, media.y + media.height / 2);
  await expect(aside).not.toHaveClass(/is-paused/);
  await expect.poll(heroName, { timeout: FEATURED_STEP_MS }).toBe("Programmable Coffee Maker");

  // Next moves on and autoplay stays on (pointer and focus stay on the button).
  await page.getByRole("button", { name: "Next featured product" }).click();
  await expect.poll(heroName).toBe("The Pragmatic Programmer");
  await expect(toggle).toHaveAttribute("aria-pressed", "false");
  await expect(aside).not.toHaveClass(/is-paused/);
  await expect.poll(heroName, { timeout: FEATURED_STEP_MS }).toBe("Adjustable Dumbbell Set");

  // Pause: the current product stays.
  await page.getByRole("button", { name: "Pause featured products" }).click();
  await expect(toggle).toHaveAttribute("aria-pressed", "true");
  await expect(aside).toHaveClass(/is-paused/);
  await expect(aside).toHaveAttribute("data-autoplay", "off");
  await page.waitForTimeout(7_500);
  expect(await heroName()).toBe("Adjustable Dumbbell Set");

  // Play: autoplay really restarts, even with the pointer still on the button.
  await page.getByRole("button", { name: "Play featured products" }).click();
  await expect(toggle).toHaveAttribute("aria-pressed", "false");
  await expect(aside).not.toHaveClass(/is-paused/);
  await expect.poll(heroName, { timeout: FEATURED_STEP_MS }).toBe("LED Desk Lamp");
});

// ------------------------------------------------------- category reel

test("category reel advances by itself and loops back to Electronics", async ({ page }) => {
  await page.goto("/");
  await page.locator("#categories").scrollIntoViewIfNeeded();
  await leaveReel(page);
  await expect.poll(() => activeCategory(page)).toBe("Electronics");
  await expect.poll(() => activeCategory(page), { timeout: REEL_STEP_MS }).toBe("Home & Kitchen");
  await expect.poll(() => activeCategory(page), { timeout: REEL_STEP_MS }).toBe("Books");

  // Jump ahead with the (secondary) arrow, then let it run on its own again.
  await page.getByRole("button", { name: "Next category" }).click();
  await page.getByRole("button", { name: "Next category" }).click();
  await expect.poll(() => activeCategory(page)).toBe("Office");
  await leaveReel(page);
  await expect.poll(() => activeCategory(page), { timeout: REEL_STEP_MS }).toBe("Electronics");
  await expect(page.locator(".reel-card:not(.is-clone)")).toHaveCount(CATEGORY_ORDER.length);
});

test("category reel: hover and arrows never pause it; Pause stops and Play resumes it", async ({ page }) => {
  test.setTimeout(90_000);
  await page.goto("/");
  const reel = page.locator(".category-reel");
  const toggle = page.locator("[data-reel-toggle]");
  await reel.scrollIntoViewIfNeeded();
  await expect.poll(() => activeCategory(page)).toBe("Electronics");
  await expect(reel).toHaveAttribute("data-autoplay", "on");

  // Hovering the reel: autoplay keeps running.
  await reel.hover();
  await expect(reel).not.toHaveClass(/is-paused/);
  await expect.poll(() => activeCategory(page), { timeout: REEL_STEP_MS }).toBe("Home & Kitchen");

  // Next and Previous move one card each; autoplay stays on afterwards.
  await page.getByRole("button", { name: "Next category" }).click();
  await expect.poll(() => activeCategory(page)).toBe("Books");
  await page.getByRole("button", { name: "Previous category" }).click();
  await expect.poll(() => activeCategory(page)).toBe("Home & Kitchen");
  await expect(toggle).toHaveAttribute("aria-pressed", "false");
  await expect(reel).not.toHaveClass(/is-paused/);
  await expect.poll(() => activeCategory(page), { timeout: REEL_STEP_MS }).toBe("Books");

  // Pause: the current card stays.
  await page.getByRole("button", { name: "Pause category animation" }).click();
  await expect(toggle).toHaveAttribute("aria-pressed", "true");
  await expect(reel).toHaveClass(/is-paused/);
  await page.waitForTimeout(6_500);
  expect(await activeCategory(page)).toBe("Books");

  // Play: autoplay really restarts, with the pointer still on the button.
  await page.getByRole("button", { name: "Play category animation" }).click();
  await expect(toggle).toHaveAttribute("aria-pressed", "false");
  await expect(reel).not.toHaveClass(/is-paused/);
  await expect.poll(() => activeCategory(page), { timeout: REEL_STEP_MS }).toBe("Fitness");
});

test.describe("reduced motion", () => {
  test.use({ reducedMotion: "reduce" });

  test("category reel does not move and becomes a manual scrollable row", async ({ page }) => {
    await page.goto("/");
    const reel = page.locator(".category-reel");
    await expect(reel).toHaveClass(/is-static/);
    const before = await page.locator("#category-track").getAttribute("style");
    await page.waitForTimeout(6_500);
    expect(await activeCategory(page)).toBe("Electronics");
    expect(await page.locator("#category-track").getAttribute("style")).toBe(before);
    await expect(page.locator(".reel-card.is-clone").first()).toBeHidden();
    await expect(page.locator("[data-reel-toggle]")).toBeHidden(); // nothing to pause
    await expect(page.getByRole("button", { name: "Next category" })).toBeVisible();
    // The hero's featured product stays put too, but can still be stepped.
    expect((await heroState(page)).name).toBe("Noise Cancelling Headset");
    await expect(page.locator("[data-feature-toggle]")).toBeHidden();
    await page.getByRole("button", { name: "Next featured product" }).click();
    await expect.poll(async () => (await heroState(page)).name).toBe("Programmable Coffee Maker");
    // No page-level horizontal scroll even though the row scrolls.
    expect(await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)).toBe(0);
  });
});

// ------------------------------------------------------------- search

test("header search filters the shop live and syncs the URL", async ({ page }) => {
  await page.goto("/shop");
  await expect(page.locator("#product-grid .product-card")).toHaveCount(58);

  await page.locator("#site-search").fill("lamp");
  await expect(page.locator("#shop-results")).toHaveText("1 result for “lamp”");
  await expect(page.locator("#product-grid .product-card__name")).toHaveText(["LED Desk Lamp"]);
  await expect(page).toHaveURL(/q=lamp/);

  await page.locator("#site-search").fill("zzz-nothing");
  await expect(page.locator("#shop-empty")).toBeVisible();
  await page.getByRole("button", { name: "Clear search and filters" }).click();
  await expect(page.locator("#product-grid .product-card")).toHaveCount(58);
});

test("searching from another page opens the filtered shop", async ({ page }) => {
  await page.goto("/about");
  await page.locator("#site-search").fill("coffee");
  await page.locator("#site-search").press("Enter");
  await expect(page).toHaveURL(/\/shop\?q=coffee/);
  await expect(page.locator("#product-grid .product-card__name")).toContainText(["Programmable Coffee Maker"]);
});

const SEARCH_CASES = [
  ["Headset", 10, "Noise Cancelling Headset", "Electronics"],
  ["Coffee", 22, "Programmable Coffee Maker", "Home & Kitchen"],
  ["Programmer", 38, "The Pragmatic Programmer", "Books"],
  ["Dumbbell", 44, "Adjustable Dumbbell Set", "Fitness"],
  ["Lamp", 51, "LED Desk Lamp", "Office"],
];

test("search dropdown shows each photographed product with its image, price and category", async ({ page }) => {
  await page.goto("/");
  const search = page.locator("#site-search");
  const panel = page.locator("#search-panel");
  for (const [query, id, name, category] of SEARCH_CASES) {
    await search.fill(query);
    await expect(panel).toBeVisible();
    await expect(search).toHaveAttribute("aria-expanded", "true");
    const first = panel.getByRole("option").first();
    await expect(first).toHaveAttribute("data-product-id", String(id));
    await expect(first.locator(".search-result__name")).toHaveText(name);
    await expect(first.locator(".search-result__meta")).toHaveText(category);
    await expect(first.locator(".search-result__price")).toHaveText(/kr\./);
    const img = first.locator("img");
    await expect(img).toHaveAttribute("src", new RegExp(`product-${id}-`));
    await expect.poll(() => img.evaluate((el) => el.complete && el.naturalWidth > 0)).toBe(true);
  }

  await search.fill("Portable Power Bank");
  await expect(panel.getByRole("option").first().locator("img")).toHaveAttribute("src", /product-11-portable-power-bank-/);
});

test("search dropdown handles no results and clearing", async ({ page }) => {
  await page.goto("/");
  const search = page.locator("#site-search");
  const panel = page.locator("#search-panel");
  await search.fill("zzz-nothing");
  await expect(panel.locator(".search-panel__empty")).toHaveText("No products match “zzz-nothing”.");
  await expect(panel.getByRole("option")).toHaveCount(0);

  await search.fill("");
  await expect(panel).toBeHidden();
  await expect(search).toHaveAttribute("aria-expanded", "false");

  await search.fill("lamp");
  await expect(panel).toBeVisible();
  await search.press("Escape");
  await expect(panel).toBeHidden();
});

test("clicking a search result opens that product", async ({ page }) => {
  await page.goto("/about");
  await page.locator("#site-search").fill("dumbbell");
  await page.locator("#search-panel").getByRole("option").first().click();
  await expect(page).toHaveURL(/\/shop\/44$/);
  await expect(page.getByRole("heading", { level: 1 })).toHaveText("Adjustable Dumbbell Set");
});

test("search dropdown works inside the mobile menu", async ({ page }) => {
  await page.setViewportSize({ width: 375, height: 812 });
  await page.goto("/");
  await page.getByRole("button", { name: "Open menu" }).click();
  await page.locator("#site-search").fill("coffee");
  const first = page.locator("#search-panel").getByRole("option").first();
  await expect(first.locator("img")).toHaveAttribute("src", /product-22-/);
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
  expect(overflow).toBe(0);
  await first.click();
  await expect(page).toHaveURL(/\/shop\/22$/);
});

// ----------------------------------------------------------- currency

test("currency selector switches, formats and persists", async ({ page }) => {
  await page.goto("/shop");
  const mouse = page.locator('.product-card[data-product-id="1"] .product-card__price');
  await expect(mouse).toHaveText(/59,99\s?kr\./);

  await page.locator("#currency-button").click();
  await page.locator('#currency-menu [data-currency="EUR"]').click();
  await expect(mouse).toHaveText("€8.04");

  await page.reload();
  await expect(page.locator("#currency-current")).toHaveText("EUR");
  await expect(page.locator('.product-card[data-product-id="1"] .product-card__price')).toHaveText("€8.04");

  await page.locator("#currency-button").click();
  await page.locator('#currency-menu [data-currency="DKK"]').click();
  await expect(page.locator('.product-card[data-product-id="1"] .product-card__price')).toHaveText(/59,99\s?kr\./);
});

// -------------------------------------------------------- add to cart

test("signed-out visitors are asked to log in instead of getting a cart", async ({ page }) => {
  const dialog = page.getByRole("dialog", { name: "Log in to shop" });

  // From a product card…
  await page.goto("/shop");
  await page.getByRole("button", { name: "Add Noise Cancelling Headset to cart" }).click();
  await expect(dialog).toBeVisible();
  await expect(dialog).toContainText("Please log in to add items to your cart.");
  await expect(dialog.getByRole("link", { name: "Log in" })).toHaveAttribute("href", "/login?next=%2Fshop");
  await expect(dialog.getByRole("link", { name: "Create account" })).toHaveAttribute("href", "/register?next=%2Fshop");
  await page.keyboard.press("Escape");
  await expect(dialog).toBeHidden();

  // …and from the product page: no cart is created either way.
  await page.goto("/shop/10");
  await page.locator("#add-to-cart").click();
  await expect(dialog).toBeVisible();
  await expect(dialog.getByRole("link", { name: "Log in" })).toHaveAttribute("href", "/login?next=%2Fshop%2F10");
  await expect(page.locator("#add-to-cart-status")).toHaveText("");
  expect(await page.evaluate(() => localStorage.getItem("cartId"))).toBeNull();
  await expect(page.locator("#cart-count")).toBeHidden();
});

test("logging in from the prompt returns to the product, where adding works", async ({ page, shopper }) => {
  await page.goto("/shop/10");
  await page.locator("#add-to-cart").click();
  await page.getByRole("dialog").getByRole("link", { name: "Log in" }).click();
  await expect(page).toHaveURL(/\/login\?next=%2Fshop%2F10$/);
  await page.locator("#email").fill(shopper.email);
  await page.locator("#password").fill(shopper.password);
  await page.getByRole("button", { name: "Log in" }).click();
  await expect(page).toHaveURL(/\/shop\/10$/);
  await page.locator("#add-to-cart").click();
  await expect(page.locator("#add-to-cart-status")).toHaveText("Added to cart.");
  await expect(page.locator("#cart-count")).toHaveText("1");
});

test("product page price follows the chosen quantity in the selected currency", async ({ page, request }) => {
  const product = await (await request.get("/products/10")).json();
  await page.addInitScript(() => localStorage.setItem("currency", "EUR"));
  await page.goto("/shop/10");
  // The same pricing code the cart uses: converted unit price × quantity.
  const expected = (qty) => page.evaluate(async ({ dkk, qty }) => {
    const currency = await import("/js/currency.js");
    const pricer = await currency.createPricer();
    return pricer.format(pricer.line(dkk, qty));
  }, { dkk: product.price, qty });

  const price = page.locator("#product-price");
  const unit = page.locator("#product-unit-price");
  expect(await expected(1)).toBe("€16.08"); // 119.99 DKK × 0.134
  await expect(price).toHaveText("€16.08");
  await expect(unit).toBeHidden();

  const increase = page.getByRole("button", { name: "Increase quantity" });
  await increase.click();
  await expect(price).toHaveText("€32.16");
  await expect(unit).toHaveText("2 × €16.08 each");
  await increase.click();
  await expect(price).toHaveText(await expected(3));
  await page.getByRole("button", { name: "Decrease quantity" }).click();
  await expect(price).toHaveText("€32.16");

  // Typing a quantity updates it too.
  await page.locator("#product-qty").fill("5");
  await expect(price).toHaveText(await expected(5));
  await page.getByRole("button", { name: "Decrease quantity" }).click();
  await page.getByRole("button", { name: "Decrease quantity" }).click();
  await page.getByRole("button", { name: "Decrease quantity" }).click();
  await page.getByRole("button", { name: "Decrease quantity" }).click();
  await expect(price).toHaveText("€16.08");
  await expect(unit).toBeHidden();
});

test("cart product photo and name open the product page", async ({ page, shopper }) => {
  await signIn(page, shopper);
  await page.goto("/shop/38");
  await page.locator("#add-to-cart").click();
  await expect(page.locator("#cart-count")).toHaveText("1");

  await page.goto("/cart");
  const row = page.locator(".cart-item", { hasText: "The Pragmatic Programmer" });
  const nameLink = row.getByRole("link", { name: "The Pragmatic Programmer" });
  const photoLink = row.locator("a.cart-item__visual-link");
  await expect(nameLink).toHaveAttribute("href", "/shop/38");
  await expect(photoLink).toHaveAttribute("href", "/shop/38");
  await expect(photoLink).toHaveAttribute("tabindex", "-1"); // one tab stop per item
  await expect(row.locator(".cart-item__qty")).toBeEditable();
  await expect(row.getByRole("button", { name: "Remove" })).toBeEnabled();

  await photoLink.click();
  await expect(page).toHaveURL(/\/shop\/38$/);
  await page.goBack();
  await nameLink.click();
  await expect(page).toHaveURL(/\/shop\/38$/);
  await expect(page.getByRole("heading", { level: 1 })).toHaveText("The Pragmatic Programmer");
});

test("add to cart from a card and from the product page updates the badge and cart", async ({ page, shopper }) => {
  await signIn(page, shopper);
  await page.goto("/shop");
  await page.getByRole("button", { name: "Add Noise Cancelling Headset to cart" }).click();
  await expect(page.locator("#cart-count")).toHaveText("1");

  await page.goto("/shop/1");
  await page.getByRole("button", { name: "Increase quantity" }).click();
  await page.getByRole("button", { name: "Increase quantity" }).click();
  await expect(page.locator("#product-qty")).toHaveValue("3");
  await page.locator("#add-to-cart").click();
  await expect(page.locator("#add-to-cart-status")).toHaveText("Added 3 to cart.");
  await expect(page.locator("#cart-count")).toHaveText("4");

  await page.goto("/cart");
  const rows = page.locator(".cart-item");
  await expect(rows).toHaveCount(2);
  await expect(page.locator(".cart-item", { hasText: "Noise Cancelling Headset" }).locator("img")).toHaveAttribute("src", /product-10-/);
  await expect(page.locator(".cart-item", { hasText: "Wireless Gaming Mouse" }).locator(".cart-item__qty")).toHaveValue("3");
});

test("add to cart from the homepage featured products updates the badge", async ({ page, shopper }) => {
  await signIn(page, shopper);
  await page.goto("/");
  const card = page.locator("#featured-grid .product-card").first();
  const name = await card.locator(".product-card__name").textContent();
  await card.getByRole("button", { name: `Add ${name} to cart` }).click();
  await expect(page.locator("#cart-count")).toHaveText("1");
  await page.goto("/cart");
  await expect(page.locator(".cart-item", { hasText: name })).toHaveCount(1);
});

// Regression: a cart id left in localStorage whose cart no longer exists on
// the server used to make every add fail silently (POST -> 404).
test("add to cart recovers from a stale cart id", async ({ page, shopper }) => {
  await signIn(page, shopper);
  const stale = "00000000-0000-4000-8000-000000000000";
  await page.goto("/shop");
  await page.evaluate((id) => localStorage.setItem("cartId", id), stale);
  await page.reload();
  await page.getByRole("button", { name: "Add LED Desk Lamp to cart" }).click();
  await expect(page.locator("#cart-count")).toHaveText("1");
  const cartId = await page.evaluate(() => localStorage.getItem("cartId"));
  expect(cartId).toBeTruthy();
  expect(cartId).not.toBe(stale);
  await page.goto("/cart");
  await expect(page.locator(".cart-item", { hasText: "LED Desk Lamp" })).toHaveCount(1);
});

// ----------------------------------------------------------- account

test("account page shows a sign-in prompt when signed out", async ({ page }) => {
  await page.goto("/orders");
  await expect(page.getByRole("heading", { level: 1 })).toHaveText("Account");
  await expect(page.locator("#account-signed-out")).toBeVisible();
  await expect(page.locator("#account-signed-out").getByRole("link", { name: "Sign in" })).toHaveAttribute("href", "/login?next=/orders");
  await expect(page.locator("#account-dashboard")).toBeHidden();
});

test("account dashboard shows the real profile, an empty order history and logs out", async ({ page, shopper }) => {
  const email = shopper.email;

  await page.goto("/login?next=/orders");
  await page.locator("#email").fill(email);
  await page.locator("#password").fill(shopper.password);
  await page.getByRole("button", { name: "Log in" }).click();
  await expect(page).toHaveURL(/\/orders$/);

  const dashboard = page.locator("#account-dashboard");
  await expect(dashboard).toBeVisible();
  await expect(page.locator("#account-name")).toHaveText("E2E Shopper");
  await expect(page.locator("#account-email")).toHaveText(email);
  await expect(page.locator("#account-avatar")).toHaveText("E");
  await expect(page.locator("#account-order-count")).toHaveText("0");
  await expect(page.locator("#orders-empty")).toBeVisible();
  await expect(page.locator("#orders-empty").getByRole("heading")).toHaveText("No orders yet");
  await expect(page.locator("#orders-empty").getByRole("link", { name: "Start shopping" })).toHaveAttribute("href", "/shop");
  await expect(dashboard.getByRole("link", { name: "View orders" })).toHaveAttribute("href", "#order-history");

  await dashboard.getByRole("button", { name: "Log out" }).click();
  await page.goto("/orders");
  await expect(page.locator("#account-signed-out")).toBeVisible();
});

test("a normal account can edit its name and email", async ({ page, request, shopper }) => {
  await signIn(page, shopper);
  await page.goto("/orders");
  await expect(page.locator("#account-card-title")).toHaveText("Account overview");
  await expect(page.locator("#account-demo-note")).toBeHidden();

  await page.getByRole("link", { name: "Edit profile" }).click();
  await expect(page).toHaveURL(/#edit-profile$/);
  await expect(page.locator("#order-history")).toBeHidden();
  const form = page.locator("#profile-form");
  await expect(form.getByLabel("Name")).toHaveValue("E2E Shopper");
  await expect(form.getByLabel("Email")).toHaveValue(shopper.email);

  const newEmail = shopper.email.replace("qa-e2e-", "qa-e2e-renamed-");
  await form.getByLabel("Name").fill("E2E Renamed");
  await form.getByLabel("Email").fill(newEmail.toUpperCase()); // stored lowercase
  await form.getByRole("button", { name: "Save changes" }).click();
  await expect(page.locator("#profile-message")).toHaveText("Profile updated.");
  await expect(page.locator("#account-name")).toHaveText("E2E Renamed");
  await expect(page.locator("#account-email")).toHaveText(newEmail);

  const me = await (await request.get("/auth/me", { headers: { Authorization: `Bearer ${shopper.token}` } })).json();
  expect(me).toEqual({ id: shopper.id, name: "E2E Renamed", email: newEmail, demoAccount: false });

  await page.getByRole("link", { name: "Back to account" }).click();
  await expect(page.locator("#order-history")).toBeVisible();
  await expect(page.locator("#account-edit")).toBeHidden();
});

test("a normal account can change its password and delete itself", async ({ page, request, shopper }) => {
  await signIn(page, shopper);
  await page.goto("/orders#edit-profile");
  const form = page.locator("#password-form");
  const message = page.locator("#password-message");
  const submit = form.getByRole("button", { name: "Change password" });
  const fill = async (current, next, confirm) => {
    await form.getByLabel("Current password").fill(current);
    await form.getByLabel("New password", { exact: true }).fill(next);
    await form.getByLabel("Confirm new password").fill(confirm);
    await submit.click();
  };
  const canLogIn = async (password) =>
    (await request.post("/auth/login", { data: { email: shopper.email, password } })).status();

  await fill(shopper.password, "New-E2e-Pass-456", "Something-else-789");
  await expect(message).toHaveText("The new passwords don't match.");

  await fill("wrong-password-1", "New-E2e-Pass-456", "New-E2e-Pass-456");
  await expect(message).toHaveText("Your current password is incorrect.");
  // A wrong current password must not look like an expired session.
  expect(await page.evaluate(() => sessionStorage.getItem("accessToken"))).toBeTruthy();

  await fill(shopper.password, "New-E2e-Pass-456", "New-E2e-Pass-456");
  await expect(message).toHaveText("Password changed.");
  expect(await canLogIn("New-E2e-Pass-456")).toBe(200);
  expect(await canLogIn(shopper.password)).toBe(401);

  // Delete (two-step confirmation), then the account is gone.
  await page.getByRole("button", { name: "Delete account" }).click();
  await page.getByRole("button", { name: "Yes, delete my account" }).click();
  await expect(page).toHaveURL(/\/$/);
  expect(await canLogIn("New-E2e-Pass-456")).toBe(401);
});

// The demo flag comes from /auth/me (the backend's DEMO_ACCOUNT_EMAIL). The
// backend's own 403s for the demo account are covered by the Maven suite
// (DemoAccountProtectionTests); this checks the page never offers edits.
test("the shared demo account is shown read-only, without Edit profile", async ({ page, shopper }) => {
  await signIn(page, shopper);
  await page.route("**/auth/me", (route) => route.fulfill({
    json: { id: shopper.id, name: "Store API Demo Customer", email: "demo@storeapi-demo.com", demoAccount: true },
  }));
  await page.goto("/orders#edit-profile");
  await expect(page.locator("#account-card-title")).toHaveText("Public demo account");
  await expect(page.locator("#account-demo-note")).toHaveText("This shared demo account can't be modified.");
  await expect(page.locator("#edit-profile-button")).toBeHidden();
  await expect(page.locator("#account-edit")).toBeHidden(); // even via the #edit-profile URL
  await expect(page.locator("#order-history")).toBeVisible();
  await expect(page.getByRole("link", { name: "View orders" })).toBeVisible();
  await expect(page.getByRole("link", { name: "Continue shopping" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Log out" })).toBeVisible();
});

// ----------------------------------------------------- product detail

test("product detail shows real data, its photo and working tabs", async ({ page, request }) => {
  const api = await (await request.get("/products/10")).json();
  await page.goto("/shop/10");
  await expect(page.getByRole("heading", { level: 1 })).toHaveText(api.name);
  await expect(page.locator("#product-desc")).toHaveText(api.description);
  await expect(page.locator("#product-category")).toContainText("Electronics");
  await expect(page.getByRole("navigation", { name: "Breadcrumb" })).toContainText(api.name);

  const img = page.locator("#product-visual img");
  await expect(img).toHaveAttribute("alt", "");
  await expect(img).toHaveAttribute("src", /product-10-noise-cancelling-headset-756\.webp$/);
  expect(await img.evaluate((el) => el.complete && el.naturalWidth > 0)).toBe(true);

  const description = page.getByRole("tab", { name: "Description" });
  await description.focus();
  await page.keyboard.press("ArrowRight");
  await expect(page.getByRole("tab", { name: "Details" })).toHaveAttribute("aria-selected", "true");
  await expect(page.locator("#product-details")).toContainText("Price in EUR");
  await expect(page.locator("#related-grid .product-card").first()).toBeVisible();

  // Every product has a photo now, but the emoji fallback must still work: a
  // product whose name doesn't match the photo mapping gets its emoji.
  await page.route("**/products/11", async (route) => {
    const product = await (await route.fetch()).json();
    await route.fulfill({ json: { ...product, name: "Portable Power Bank 10000mAh" } });
  });
  await page.goto("/shop/11");
  await expect(page.locator("#product-visual .product-visual-emoji")).toBeVisible();
  await expect(page.locator("#product-visual img")).toHaveCount(0);
});

// ---------------------------------------------------- layout, status

test("no page-level horizontal overflow at the supported widths", async ({ page }) => {
  for (const width of [1440, 1024, 768, 375, 320]) {
    await page.setViewportSize({ width, height: 900 });
    for (const path of ["/", "/shop", "/shop/10", "/cart", "/about", "/orders"]) {
      await page.goto(path);
      await page.waitForLoadState("networkidle");
      const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
      expect(overflow, `${path} at ${width}px`).toBe(0);
    }
  }
});

test("mobile menu opens with the nav, search and service status", async ({ page }) => {
  await page.setViewportSize({ width: 375, height: 812 });
  await page.goto("/");
  await expect(page.locator(".primary-nav")).toBeHidden();
  await page.getByRole("button", { name: "Open menu" }).click();
  await expect(page.getByRole("navigation", { name: "Main" }).getByRole("link", { name: "About" })).toBeVisible();
  await expect(page.locator("#site-search")).toBeVisible();
  await expect(page.locator("#server-status-text")).toHaveText("Server online");
  await page.keyboard.press("Escape");
  await expect(page.locator(".primary-nav")).toBeHidden();
});

test("service status shows offline when the health check fails", async ({ page }) => {
  await page.route("**/actuator/health", (route) => route.fulfill({ status: 503, body: '{"status":"DOWN"}', contentType: "application/json" }));
  await page.goto("/");
  await expect(page.locator("#server-status-text")).toHaveText("Server offline");
});
