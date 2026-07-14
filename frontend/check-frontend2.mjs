import { chromium } from 'playwright';

const browser = await chromium.launch();
const context = await browser.newContext();
const page = await context.newPage();

page.on('console', msg => console.log('CONSOLE:', msg.type(), msg.text()));
page.on('pageerror', err => console.log('PAGEERROR:', err.message));
page.on('request', req => {
  if (req.url().includes('/api/')) {
    console.log('REQUEST:', req.method(), req.url());
  }
});
page.on('response', async res => {
  if (res.url().includes('/api/')) {
    const status = res.status();
    let body = '';
    try {
      body = await res.text();
    } catch (e) {}
    console.log('RESPONSE:', status, res.url(), body.substring(0, 120));
  }
});

await page.goto('http://localhost:15173/', { waitUntil: 'networkidle' });
await page.waitForTimeout(5000);

// Check DOM for data
const guidesCount = await page.locator('text=External Channel').count();
const resourcesCount = await page.locator('text=Public Client').count();
const activitiesCount = await page.locator('text=北冥公开活动').count();
const changelogCount = await page.locator('text=北冥服务器种子更新').count();

console.log('DOM checks:', { guidesCount, resourcesCount, activitiesCount, changelogCount });

const screenshot = await page.screenshot({ fullPage: true });
const fs = await import('fs');
fs.writeFileSync('/tmp/frontend-screenshot2.png', screenshot);

await browser.close();
