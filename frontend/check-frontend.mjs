import { chromium } from 'playwright';

const browser = await chromium.launch();
const page = await browser.newPage();
page.on('console', msg => console.log('CONSOLE:', msg.type(), msg.text()));
page.on('pageerror', err => console.log('PAGEERROR:', err.message));
page.on('requestfailed', req => console.log('REQUESTFAILED:', req.url(), req.failure()?.errorText));

await page.goto('http://localhost:15173/', { waitUntil: 'networkidle' });
await page.waitForTimeout(3000);

const screenshot = await page.screenshot({ fullPage: true });
const fs = await import('fs');
fs.writeFileSync('/tmp/frontend-screenshot.png', screenshot);

console.log('Page title:', await page.title());
console.log('Page URL:', page.url());

await browser.close();
