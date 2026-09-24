# Store API — product brief

## What it is

Store API – Spring Boot E-Commerce Backend is a portfolio e-commerce
application: a Spring Boot REST API (JWT auth with roles, product catalog,
UUID-identified carts, Stripe Checkout with webhooks, orders, DKK/EUR
pricing, Flyway-versioned MySQL) with a server-rendered storefront
(Thymeleaf + vanilla JavaScript) built on top of that same public API.

## Who it's for

- **Shoppers** browsing the storefront: find a product, sign in (or create
  an account), add it to the cart, pay with Stripe, see the order afterwards
  and manage their own profile. Browsing needs no account; adding to the
  cart and checking out do.
- **Reviewers of the portfolio** (recruiters, engineers): they should see a
  store that feels like a real, premium shop *and* be able to reach the API
  (Swagger docs, source) and understand what was built.

## Experience

- Premium, dark, cinematic, product-focused. Large product photography,
  electric-blue accents on deep navy, restrained glow.
- Signature interaction: the home page's category reel moves through the
  five categories by itself, like a showroom turntable, and the hero's
  featured-product reel does the same for five products. Hovering or using
  the arrows never stops them; only their Pause button does, and Play
  resumes them.
- Every price is real and follows the visitor's DKK/EUR choice, including
  the product page price for the chosen quantity; completed orders keep the
  currency they were charged in.
- Accounts: a normal account can edit its name and email, change its
  password, and delete itself while it has no orders. The shared public demo
  account (`demo@storeapi-demo.com`) is read-only, and there is no public
  admin account.

## Truth rules

- Only show what the backend actually knows: product names, descriptions,
  prices, categories, cart, orders, payment status, service health.
- No invented ratings, reviews, testimonials, stock levels, shipping or
  return promises, company details, or support SLAs.
- Product photography is AI-generated and original to this project; all 58
  products have a photo. The emoji/category visual remains only as a
  fallback for a product without a matching photo.

## Capabilities the UI can rely on

Product list/detail API, cart API (UUID-identified; the storefront only
uses it for signed-in shoppers), JWT login/register/refresh, `GET /auth/me`
(including the `demoAccount` flag), account self-service (update name and
email, change password, delete), Stripe Checkout (DKK or EUR), order history
per user, `GET /currencies` rates, `GET /actuator/health`.
