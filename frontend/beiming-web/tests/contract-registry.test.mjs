import { readFile, readdir } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../..");
const docsDir = path.join(root, "docs");
const registryPath = path.join(root, "frontend/beiming-web/src/api/registry.ts");
const expectedTotal = 746;

function moduleFromFile(file) {
  return file.replace(/^contracts-/, "").replace(/\.md$/, "");
}

async function parseContractEndpoints() {
  const files = (await readdir(docsDir))
    .filter((file) => /^contracts-.*\.md$/.test(file))
    .filter((file) => !["contracts-common.md", "contracts-frontend-adaptation.md"].includes(file))
    .sort();
  const endpoints = [];
  for (const file of files) {
    const text = await readFile(path.join(docsDir, file), "utf8");
    const regex = /`(GET|POST|PUT|PATCH|DELETE)\s+([^`]+)`/g;
    let match;
    while ((match = regex.exec(text))) {
      const route = match[2].trim();
      if (!route.startsWith("/")) {
        continue;
      }
      endpoints.push({
        moduleKey: moduleFromFile(file),
        method: match[1],
        pathTemplate: route
      });
    }
  }
  const unique = new Map();
  for (const endpoint of endpoints) {
    unique.set(`${endpoint.moduleKey} ${endpoint.method} ${endpoint.pathTemplate}`, endpoint);
  }
  return [...unique.values()];
}

function extractRegistryEntries(source) {
  const entries = [];
  const regex = /"?method"?:\s*"([^"]+)"[\s\S]*?"?pathTemplate"?:\s*"([^"]+)"[\s\S]*?"?moduleKey"?:\s*"([^"]+)"[\s\S]*?"?pageRoute"?:\s*"([^"]+)"[\s\S]*?"?testCaseGroup"?:\s*"([^"]+)"/g;
  let match;
  while ((match = regex.exec(source))) {
    entries.push({
      method: match[1],
      pathTemplate: match[2],
      moduleKey: match[3],
      pageRoute: match[4],
      testCaseGroup: match[5]
    });
  }
  return entries;
}

const endpoints = await parseContractEndpoints();
if (endpoints.length !== expectedTotal) {
  throw new Error(`FEA-REG-002 expected ${expectedTotal} endpoints, got ${endpoints.length}`);
}

let registrySource = "";
try {
  registrySource = await readFile(registryPath, "utf8");
} catch {
  throw new Error("FEA-REG-003 registry file is missing at frontend/beiming-web/src/api/registry.ts");
}

const entries = extractRegistryEntries(registrySource);
const registryKeys = new Set(entries.map((entry) => `${entry.moduleKey} ${entry.method} ${entry.pathTemplate}`));
const contractKeys = new Set(endpoints.map((endpoint) => `${endpoint.moduleKey} ${endpoint.method} ${endpoint.pathTemplate}`));
const missing = endpoints.filter((endpoint) => !registryKeys.has(`${endpoint.moduleKey} ${endpoint.method} ${endpoint.pathTemplate}`));
const extra = entries.filter((entry) => !contractKeys.has(`${entry.moduleKey} ${entry.method} ${entry.pathTemplate}`));

if (missing.length > 0) {
  throw new Error(`FEA-REG-003 registry missing ${missing.length} endpoints, first: ${missing[0].method} ${missing[0].pathTemplate}`);
}
if (extra.length > 0) {
  throw new Error(`FEA-REG-004 registry has ${extra.length} extra endpoints, first: ${extra[0].method} ${extra[0].pathTemplate}`);
}

for (const entry of entries) {
  if (!entry.pageRoute || !entry.testCaseGroup) {
    throw new Error(`FEA-REG-007/014 registry entry incomplete: ${entry.method} ${entry.pathTemplate}`);
  }
}

console.log(`frontend API registry covers ${entries.length} endpoints`);
