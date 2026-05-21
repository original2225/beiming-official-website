import { spawn } from "node:child_process";
import { access } from "node:fs/promises";
import { setTimeout as wait } from "node:timers/promises";
import { chromium } from "playwright-core";

const rootUrl = process.env.AUTH_CONSOLE_URL ?? "http://127.0.0.1:5174";
const runWithBackend = process.argv.includes("--with-backend");
const edgeCandidates = [
  "C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe",
  "C:/Program Files/Microsoft/Edge/Application/msedge.exe"
];

async function fileExists(path) {
  try {
    await access(path);
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
      // Wait for Vite to start.
    }
    await wait(500);
  }
  throw new Error(`Timed out waiting for ${url}`);
}

async function startViteIfNeeded() {
  if (process.env.AUTH_CONSOLE_URL) {
    return null;
  }
  try {
    const response = await fetch(rootUrl);
    if (response.ok) {
      return null;
    }
  } catch {
    // Start a local Vite server below.
  }
  return spawn("npx", ["vite", "--host", "127.0.0.1", "--port", "5174", "--strictPort"], {
    cwd: new URL("..", import.meta.url),
    shell: true,
    stdio: "pipe"
  });
}

async function loginBackend() {
  const response = await fetch("http://127.0.0.1:8101/api/v1/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username: "owner", password: "Password12345" })
  });
  if (!response.ok) {
    throw new Error(`auth backend login failed with HTTP ${response.status}`);
  }
}

async function startBackendIfNeeded() {
  if (!runWithBackend) {
    return null;
  }
  try {
    await loginBackend();
    return null;
  } catch {
    // Start the local auth service below.
  }
  const backend = spawn("mvn", ["spring-boot:run"], {
    cwd: new URL("../../../backend/auth-service/", import.meta.url),
    shell: true,
    stdio: "pipe"
  });
  for (let attempt = 0; attempt < 60; attempt += 1) {
    try {
      await wait(1000);
      await loginBackend();
      return backend;
    } catch {
      // Wait for Spring Boot to finish starting.
    }
  }
  backend.kill();
  throw new Error("Timed out waiting for auth backend");
}

async function clickNav(page, name, expectedHeading) {
  await page.getByRole("navigation", { name: "测试台导航" }).getByRole("button", { name, exact: true }).click();
  await page.waitForTimeout(200);
  await page.waitForFunction((label) => {
    const current = document.querySelector("[aria-current='page']");
    return current?.textContent?.includes(label);
  }, name);
  await page.getByRole("heading", { name: expectedHeading }).scrollIntoViewIfNeeded();
  await page.getByRole("heading", { name: expectedHeading }).waitFor({ state: "visible" });
}

async function expectNoTopbarOverlap(page) {
  const boxes = await page.locator(".topbar > *").evaluateAll((items) =>
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
        throw new Error(`Topbar overlap detected between "${a.text}" and "${b.text}"`);
      }
    }
  }
}

async function expectAuthRoleContractUi(page) {
  await page.getByText("基础角色模型").waitFor();
  await page.getByText("OWNER / ADMIN / HELPER / USER").waitFor();
  await page.getByText("运维能力点").waitFor();
  for (const permission of [
    "NODE_READ",
    "NODE_WRITE",
    "CONTAINER_OPERATE",
    "VM_OPERATE",
    "FILE_MANAGE",
    "TERMINAL_ACCESS",
    "HIGH_RISK_APPROVE"
  ]) {
    await page.locator(".check-option", { hasText: permission }).waitFor();
  }

  const bodyText = await page.locator("body").innerText();
  if (bodyText.includes("Guest") || bodyText.includes("测试管理员")) {
    throw new Error("Non-contract user labels are still visible");
  }

  const roleOptions = await page.locator("label", { hasText: "绑定角色" }).locator("option").evaluateAll((options) =>
    options.map((option) => option.textContent?.trim())
  );
  if (roleOptions.join("|") !== "USER") {
    throw new Error(`PLAYER invitation role options must be USER only, got ${roleOptions.join(",")}`);
  }
  const disabledPermissionCount = await page.locator(".check-group input:disabled").count();
  if (disabledPermissionCount !== 7) {
    throw new Error(`PLAYER invitation must not allow operation permissions, got ${disabledPermissionCount} disabled permission controls`);
  }

  await page.locator("label", { hasText: "类型" }).locator("select").selectOption("ADMIN");
  const adminRoleOptions = await page.locator("label", { hasText: "绑定角色" }).locator("option").evaluateAll((options) =>
    options.map((option) => option.textContent?.trim())
  );
  if (adminRoleOptions.join("|") !== "ADMIN|HELPER") {
    throw new Error(`ADMIN invitation role options must be ADMIN and HELPER, got ${adminRoleOptions.join(",")}`);
  }
  if (adminRoleOptions.includes("OWNER")) {
    throw new Error("Invitation role options must not include OWNER");
  }
  const enabledPermissionCount = await page.locator(".check-group input:not(:disabled)").count();
  if (enabledPermissionCount !== 7) {
    throw new Error(`ADMIN invitation must expose the exact operation permission controls, got ${enabledPermissionCount}`);
  }
}

async function run() {
  const backend = await startBackendIfNeeded();
  const vite = await startViteIfNeeded();
  try {
    await waitForUrl(rootUrl);
    const browser = await chromium.launch({ headless: true, executablePath: await findEdge() });
    const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
    await page.goto(rootUrl);
    await page.evaluate(() => localStorage.clear());
    await page.goto(rootUrl, { waitUntil: "networkidle" });

    await page.getByRole("heading", { name: "统一测试注册、登录与会话校验" }).waitFor();
    await page.getByRole("heading", { name: "接口操作" }).waitFor();
    await page.getByRole("heading", { name: "邀请码管理" }).waitFor();
    await page.getByRole("heading", { name: "最近事件" }).waitFor();
    await expectNoTopbarOverlap(page);
    await page.setViewportSize({ width: 1988, height: 1000 });
    await expectNoTopbarOverlap(page);
    await page.setViewportSize({ width: 1440, height: 1000 });
    await expectAuthRoleContractUi(page);

    await clickNav(page, "概览", "统一测试注册、登录与会话校验");
    await clickNav(page, "账号", "接口操作");
    await clickNav(page, "会话", "北冥认证器");
    await clickNav(page, "邀请码", "邀请码管理");
    await clickNav(page, "日志", "最近事件");

    if (runWithBackend) {
      await page.getByRole("button", { name: /快速登录/ }).click();
      await page.waitForFunction(() => document.body.innerText.includes("owner") && document.body.innerText.includes("READY"));

      await page.getByRole("button", { name: /^当前用户$/ }).click();
      await page.waitForFunction(() => document.body.innerText.includes('"username": "owner"'));

      await page.getByRole("button", { name: /^会话校验$/ }).click();
      await page.waitForFunction(() => document.body.innerText.includes('"valid": true'));

      await page.getByRole("button", { name: /^用户列表$/ }).click();
      await page.waitForFunction(() => document.body.innerText.includes('"items"') && document.body.innerText.includes('"username": "admin"'));

      await page.locator(".quick-actions").getByRole("button", { name: /^邀请码$/ }).click();
      await page.waitForFunction(() => document.body.innerText.includes("PLAYER") && document.body.innerText.includes("ADMIN"));

      await clickNav(page, "邀请码", "邀请码管理");
      await page.getByRole("button", { name: /创建邀请码/ }).click();
      await page.waitForFunction(() => document.body.innerText.includes("BM-") && document.body.innerText.includes("ACTIVE"));

      await page.locator(".icon-action:not([disabled])").first().click();
      await page.waitForFunction(() => document.body.innerText.includes("DISABLED"));

      await page.getByRole("button", { name: /^退出$/ }).click();
      await page.waitForFunction(() => document.body.innerText.includes("未登录") || document.body.innerText.includes("EMPTY"));
    }

    await browser.close();
    console.log("auth test console e2e passed");
  } finally {
    if (vite) {
      vite.kill();
    }
    if (backend) {
      backend.kill();
    }
  }
}

run().catch((error) => {
  console.error(error);
  process.exit(1);
});
