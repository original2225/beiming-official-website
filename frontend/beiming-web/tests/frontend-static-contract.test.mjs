import { readFile, readdir, stat } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../..");
const appDir = path.join(root, "frontend/beiming-web");
const srcDir = path.join(appDir, "src");
const viteConfigPath = path.join(appDir, "vite.config.ts");
const localTestsPath = path.join(root, ".local-docs/tests-frontend-adaptation.md");
const forbidden = [
  "X-Beiming-Actor-",
  "X-Gateway-Internal-",
  "Bearer secret-token",
  "BM-SECRET-RAW",
  "cloudrevePassword=secret-code",
  "node-token-raw",
  "terminal command body"
];

async function walk(dir) {
  const items = [];
  for (const name of await readdir(dir)) {
    const item = path.join(dir, name);
    const info = await stat(item);
    if (info.isDirectory()) {
      items.push(...await walk(item));
    } else if (/\.(ts|tsx|css|html|mjs)$/.test(name)) {
      items.push(item);
    }
  }
  return items;
}

await readFile(localTestsPath, "utf8");

const viteConfig = await readFile(viteConfigPath, "utf8").catch(() => {
  throw new Error("FEA-SEC-020 vite.config.ts is missing");
});
if (!viteConfig.includes("5175")) {
  throw new Error("FEA-SEC-020 Vite dev server must use port 5175");
}

const files = await walk(srcDir).catch(() => {
  throw new Error("FEA-SEC source directory is missing");
});
const combined = (await Promise.all(files.map((file) => readFile(file, "utf8")))).join("\n");
const client = await readFile(path.join(srcDir, "api/client.ts"), "utf8");
const app = await readFile(path.join(srcDir, "App.tsx"), "utf8");

if (!combined.includes("8125")) {
  throw new Error("FEA-SEC-003 default API base must point at gateway port 8125");
}
if (!combined.includes("VITE_BEIMING_API_MODE")) {
  throw new Error("FEA-SEC-004 direct API mode must be explicitly env controlled");
}
if (client.includes('?? "success"') || client.includes("?? 'success'")) {
  throw new Error("FEA-SEC mock mode must not be enabled by default without an explicit query parameter");
}
for (const match of app.matchAll(/requestEnvelope<[^>]+>\("([^"]+)"/g)) {
  if (!match[1].startsWith("/api/v1/")) {
    throw new Error(`FEA-API page request must use an official API contract path: ${match[1]}`);
  }
}
for (const expected of ["/api/v1/content/home", "/api/v1/auth/session/verify", "/api/v1/admin/overview", "/api/v1/ops-control/overview"]) {
  if (!app.includes(expected)) {
    throw new Error(`FEA-API expected official page adapter path is missing: ${expected}`);
  }
}
for (const needle of forbidden) {
  if (combined.includes(needle)) {
    throw new Error(`FEA-SEC sensitive or trusted header string must not appear in source: ${needle}`);
  }
}

console.log(`frontend static contract passed across ${files.length} files`);
