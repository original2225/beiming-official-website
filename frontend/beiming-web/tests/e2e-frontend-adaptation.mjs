import { spawn } from "node:child_process";
import { access } from "node:fs/promises";
import { setTimeout as wait } from "node:timers/promises";
import { fileURLToPath } from "node:url";
import { chromium } from "playwright-core";

const appUrl = process.env.BEIMING_WEB_URL ?? "http://127.0.0.1:5175";
const projectRoot = fileURLToPath(new URL("..", import.meta.url));
const viteBin = fileURLToPath(new URL("../node_modules/vite/bin/vite.js", import.meta.url));
const edgeCandidates = [
  "C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe",
  "C:/Program Files/Microsoft/Edge/Application/msedge.exe"
];

async function fileExists(candidate) {
  try {
    await access(candidate);
    return true;
  } catch {
    return false;
  }
}

async function findEdge() {
  for (const candidate of edgeCandidates) {
    if (await fileExists(candidate)) {
      return candidate;
    }
  }
  throw new Error("Microsoft Edge executable was not found");
}

async function waitForUrl(url) {
  for (let attempt = 0; attempt < 40; attempt += 1) {
    try {
      const response = await fetch(url);
      if (response.ok) {
        return;
      }
    } catch {
      await wait(500);
    }
  }
  throw new Error(`Timed out waiting for ${url}`);
}

async function startViteIfNeeded() {
  if (process.env.BEIMING_WEB_URL) {
    return null;
  }
  try {
    const response = await fetch(appUrl);
    if (response.ok) {
      return null;
    }
  } catch {
    // Start local Vite below.
  }
  return spawn(process.execPath, [viteBin, "--host", "127.0.0.1", "--port", "5175", "--strictPort"], {
    cwd: projectRoot,
    stdio: "pipe"
  });
}

async function expectNoOverlap(page, selector) {
  const boxes = await page.locator(selector).evaluateAll((items) =>
    items.map((item) => {
      const rect = item.getBoundingClientRect();
      return {
        text: item.textContent,
        left: rect.left,
        right: rect.right,
        top: rect.top,
        bottom: rect.bottom
      };
    })
  );
  for (let index = 0; index < boxes.length; index += 1) {
    for (let next = index + 1; next < boxes.length; next += 1) {
      const a = boxes[index];
      const b = boxes[next];
      const overlapX = Math.max(0, Math.min(a.right, b.right) - Math.max(a.left, b.left));
      const overlapY = Math.max(0, Math.min(a.bottom, b.bottom) - Math.max(a.top, b.top));
      if (overlapX > 1 && overlapY > 1) {
        throw new Error(`Layout overlap detected between "${a.text}" and "${b.text}"`);
      }
    }
  }
}

const vite = await startViteIfNeeded();
let browser;
try {
  await waitForUrl(appUrl);
  browser = await chromium.launch({ headless: true, executablePath: await findEdge() });
  const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
  await page.goto(`${appUrl}/?mock=success`, { waitUntil: "networkidle" });

  await page.getByRole("heading", { name: "北冥官网" }).waitFor();
  await page.getByLabel("主导航").getByRole("link", { name: "资源中心" }).click();
  await page.getByRole("heading", { name: "资源中心" }).waitFor();
  await page.getByLabel("主导航").getByRole("link", { name: "指南知识库" }).click();
  await page.getByRole("heading", { name: "指南知识库" }).waitFor();
  await page.getByLabel("主导航").getByRole("link", { name: "登录" }).click();
  await page.getByRole("heading", { name: "登录" }).waitFor();

  await page.goto(`${appUrl}/?mock=degraded`, { waitUntil: "networkidle" });
  await page.getByText("局部降级").waitFor();
  await page.getByText("req_mock").waitFor();

  await page.goto(`${appUrl}/admin?mock=admin`, { waitUntil: "networkidle" });
  await page.getByRole("heading", { name: "管理后台" }).waitFor();
  await page.getByText("API_GATEWAY").first().waitFor();

  await page.goto(`${appUrl}/ops?mock=ops`, { waitUntil: "networkidle" });
  await page.getByRole("heading", { name: "运维控制台" }).waitFor();
  await page.getByText("SIMULATED").waitFor();

  await page.goto(`${appUrl}/me`, { waitUntil: "networkidle" });
  await page.getByRole("heading", { name: "用户中心" }).waitFor();
  await page.getByText("需要登录").waitFor();
  if ((await page.locator("body").innerText()).includes("北冥管理者")) {
    throw new Error("Unauthenticated real-mode user center leaked mock owner identity");
  }

  await page.goto(`${appUrl}/admin`, { waitUntil: "networkidle" });
  await page.getByRole("heading", { name: "管理后台" }).waitFor();
  await page.getByText("后台暂不可用").waitFor();
  if ((await page.locator("body").innerText()).includes("API_GATEWAY")) {
    throw new Error("Unauthenticated real-mode admin page leaked mock platform data");
  }

  await expectNoOverlap(page, "[data-layout-check='topbar'] > *");
  await page.setViewportSize({ width: 390, height: 900 });
  await page.goto(`${appUrl}/?mock=success`, { waitUntil: "networkidle" });
  await expectNoOverlap(page, "[data-layout-check='mobile-stack'] > *");
  const bodyText = await page.locator("body").innerText();
  for (const forbidden of ["Bearer secret-token", "BM-SECRET-RAW", "node-token-raw", "terminal command body"]) {
    if (bodyText.includes(forbidden)) {
      throw new Error(`Sensitive value leaked into page: ${forbidden}`);
    }
  }
  console.log("frontend adaptation e2e passed");
} finally {
  await browser?.close();
  if (vite) {
    vite.kill();
  }
}
