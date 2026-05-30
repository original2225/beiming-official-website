import { readFile } from "node:fs/promises";

const harness = await readFile(new URL("./e2e-frontend-adaptation.mjs", import.meta.url), "utf8");

if (harness.includes("shell: true")) {
  throw new Error("e2e harness must not start Vite through a shell because the Vite child process can survive cleanup");
}

if (!harness.includes("process.execPath") || !harness.includes("vite/bin/vite.js")) {
  throw new Error("e2e harness must start Vite through the current Node executable and Vite bin entrypoint");
}

if (!harness.includes("await browser?.close()")) {
  throw new Error("e2e harness must close Playwright from the final cleanup path");
}

console.log("e2e harness contract passed");
