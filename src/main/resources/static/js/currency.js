// Currency selection, conversion and price formatting for the storefront.
// Every price on the site goes through this module — no page script formats
// money or knows a currency symbol itself.
//
// Product prices from the API are always DKK (the store's base currency).
// Other currencies are derived with the rates from GET /currencies, which
// are the same configured demo rates checkout sends to Stripe.

const STORAGE_KEY = "currency";

export const SUPPORTED_CURRENCIES = ["DKK", "EUR"];
export const DEFAULT_CURRENCY = "DKK";

const LOCALES = {
  DKK: "da-DK", // 59,99 kr.
  EUR: "en-IE", // €8.04
};

// The saved choice, or DKK when nothing (or anything unexpected) is stored.
export function getCurrency() {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    return SUPPORTED_CURRENCIES.includes(stored) ? stored : DEFAULT_CURRENCY;
  } catch {
    return DEFAULT_CURRENCY;
  }
}

export function setCurrency(code) {
  if (!SUPPORTED_CURRENCIES.includes(code)) return;
  try {
    localStorage.setItem(STORAGE_KEY, code);
  } catch {
    // Storage unavailable: the choice still applies to this page view.
  }
  window.dispatchEvent(new CustomEvent("currencychange", { detail: { currency: code } }));
}

// Pages re-render their prices when the header selector changes currency.
export function onCurrencyChange(callback) {
  window.addEventListener("currencychange", () => callback(getCurrency()));
}

let ratesPromise = null;

export function loadRates() {
  if (!ratesPromise) {
    ratesPromise = fetch("/currencies", { headers: { Accept: "application/json" } })
      .then((response) => {
        if (!response.ok) throw new Error(`Rates request failed with ${response.status}`);
        return response.json();
      })
      .then((data) => data.rates)
      .catch((err) => {
        ratesPromise = null; // allow a retry on the next call
        throw err;
      });
  }
  return ratesPromise;
}

// Converts a DKK amount to an integer number of minor units (øre / cents)
// in `currency`. Mirrors CurrencyService#convertFromBase exactly: the rate
// is applied with integer (BigInt) arithmetic and rounded half-up to two
// decimals, so no floating-point error can make the site disagree with
// what Stripe charges.
export function toMinorUnits(dkkAmount, currency, rates) {
  const baseMinor = BigInt(Math.round(Number(dkkAmount) * 100));
  const [whole, fraction = ""] = String(rates[currency]).split(".");
  const rateDigits = BigInt(whole + fraction);
  const divisor = 10n ** BigInt(fraction.length);
  return Number((baseMinor * rateDigits * 2n + divisor) / (2n * divisor));
}

export function formatMinorUnits(minorUnits, currency) {
  return new Intl.NumberFormat(LOCALES[currency], { style: "currency", currency }).format(minorUnits / 100);
}

// For amounts that are already in a known currency — i.e. a placed order,
// which records what it was charged in. Never converted, and never affected
// by the visitor's current currency preference.
export function formatAmount(amount, currency) {
  const code = SUPPORTED_CURRENCIES.includes(currency) ? currency : DEFAULT_CURRENCY;
  return formatMinorUnits(Math.round(Number(amount) * 100), code);
}

// Returns a small helper bound to the current currency + loaded rates, so
// a page can price a whole list without re-reading storage each time.
export async function createPricer() {
  const rates = await loadRates();
  const currency = getCurrency();
  const unit = (dkkAmount) => toMinorUnits(dkkAmount, currency, rates);
  return {
    currency,
    rate: rates[currency],
    unit,
    // Line total = converted unit price × quantity (same as Stripe).
    line: (dkkUnitPrice, quantity) => unit(dkkUnitPrice) * quantity,
    format: (minorUnits) => formatMinorUnits(minorUnits, currency),
    formatPrice: (dkkAmount) => formatMinorUnits(unit(dkkAmount), currency),
  };
}
