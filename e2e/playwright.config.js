// Browser tests for the Store API storefront.
//
// They run against an app that is already running (they don't start it,
// because it needs MySQL and the .env secrets):
//
//   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   (repo root)
//   cd e2e && npm install && npx playwright test
//
// Override the target with STORE_BASE_URL=https://… if needed.
const { defineConfig, devices } = require("@playwright/test");

module.exports = defineConfig({
  testDir: "./tests",
  timeout: 45_000,
  expect: { timeout: 8_000 },
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [["list"]],
  use: {
    baseURL: process.env.STORE_BASE_URL || "http://localhost:8080",
    trace: "retain-on-failure",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"], viewport: { width: 1440, height: 900 } } }],
});
