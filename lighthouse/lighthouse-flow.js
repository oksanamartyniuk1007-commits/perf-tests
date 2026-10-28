const puppeteer = require('puppeteer');
const fs = require('fs');

const CONFIG = {
  baseUrl: process.env.BASE_URL || 'http://localhost',
  debuggingPort: 9222,
  reportName: process.env.REPORT_NAME || 'lighthouse-flow-report.html',
  protocolTimeout: 300000,
  actionTimeout: 15000,
  selectorTimeout: 15000,
  headless: process.env.HEADLESS !== 'false'
};

const TEST_CUSTOMER = {
  fullName: 'John Doe',
  address: '123 Main Street',
  postalCode: '01001',
  city: 'Kyiv',
  countryCode: 'UA',
  phone: '+380501234567',
  email: 'test@example.com'
};

const SELECTORS = {
  menu: {
    tablesTab: '.right-menu li.page_item > a[href*="tables"]',
    cartTab: '.right-menu li.page_item > a[href*="/cart"]'
  },
  products: {
    firstProductCard: '.product-list .al_archive > a > img',
    addToCartBtn: '[id^="reg_add_"] button[type="submit"]'
  },
  cart: {
    placeOrderOpenBtn: '#shopping-cart-container .form-buttons input[type="submit"]'
  },
  cartForm: {
    formContainer: '#shopping-cart-submit-container',
    fullName: '#shopping-cart-submit-container form input[name="cart_name"]',
    address: '#shopping-cart-submit-container form input[name="cart_address"]',
    postalCode: '#shopping-cart-submit-container form input[name="cart_postal"]',
    city: '#shopping-cart-submit-container form input[name="cart_city"]',
    countryDropdown: '#shopping-cart-submit-container form select',
    phone: '#shopping-cart-submit-container form input[name="cart_phone"]',
    email: '#shopping-cart-submit-container form .email input',
    placeOrderBtn: '#shopping-cart-submit-container form input[type="submit"]'
  }
};

async function waitForSelector(page, selector, timeout = CONFIG.selectorTimeout) {
  await page.waitForSelector(selector, { visible: true, timeout });
}

async function waitTillHTMLRendered(page, timeout = 30000) {
  const checkDelay = 500;
  let lastLength = 0;
  let stableChecks = 0;
  const start = Date.now();

  while (Date.now() - start < timeout) {
    let currentLength;
    try {
      const html = await page.content();
      currentLength = html.length;
    } catch (err) {
      stableChecks = 0;
      await new Promise(resolve => setTimeout(resolve, checkDelay));
      continue;
    }

    if (currentLength === lastLength) {
      stableChecks++;
      if (stableChecks >= 3) return;
    } else {
      stableChecks = 0;
      lastLength = currentLength;
    }

    await new Promise(resolve => setTimeout(resolve, checkDelay));
  }
}

async function clickViaLocator(page, selector, timeout = CONFIG.actionTimeout) {
  await waitForSelector(page, selector, timeout);
  await page.locator(selector).setTimeout(timeout).click();
}

async function fillViaLocator(page, selector, text, timeout = CONFIG.actionTimeout) {
  await waitForSelector(page, selector, timeout);
  await page.locator(selector).setTimeout(timeout).fill(text);
}

async function selectOption(page, selector, value, timeout = CONFIG.actionTimeout) {
  await waitForSelector(page, selector, timeout);
  await page.select(selector, value);
}

async function safeNavigationWait(page) {
  await page.waitForNavigation({ waitUntil: 'networkidle2' }).catch(() => {});
  await waitTillHTMLRendered(page);
}

async function fillCheckoutForm(page, customerData) {
  await fillViaLocator(page, SELECTORS.cartForm.fullName, customerData.fullName);
  await fillViaLocator(page, SELECTORS.cartForm.address, customerData.address);
  await fillViaLocator(page, SELECTORS.cartForm.postalCode, customerData.postalCode);
  await fillViaLocator(page, SELECTORS.cartForm.city, customerData.city);
  await selectOption(page, SELECTORS.cartForm.countryDropdown, customerData.countryCode);
  await fillViaLocator(page, SELECTORS.cartForm.phone, customerData.phone);
  await fillViaLocator(page, SELECTORS.cartForm.email, customerData.email);
}

async function runLighthouseFlow() {
  console.log('Starting browser...');
  const { startFlow } = await import('lighthouse');
  const desktopConfig = (await import('lighthouse/core/config/desktop-config.js')).default;

  const browser = await puppeteer.launch({
    headless: CONFIG.headless,
    defaultViewport: { width: 1920, height: 1080 },
    protocolTimeout: CONFIG.protocolTimeout,
    args: [
      `--remote-debugging-port=${CONFIG.debuggingPort}`,
      '--no-sandbox',
      '--disable-setuid-sandbox',
      '--disable-dev-shm-usage'
    ],
  });

  try {
    const [page] = await browser.pages();

    const flow = await startFlow(page, {
      name: 'Performance Testing Essentials - Desktop User Flow',
      config: desktopConfig,
      configContext: {
        settingsOverrides: {
          onlyCategories: ['performance']
        }
      },
    });

    console.log('Step 1: Open Application');
    await flow.navigate(CONFIG.baseUrl, { name: '1. Open Application' });
    await waitTillHTMLRendered(page);

    console.log('Step 2: Navigate to Tables Tab');
    await flow.startTimespan({ name: '2. Navigate to Tables Tab' });
    await clickViaLocator(page, SELECTORS.menu.tablesTab);
    await safeNavigationWait(page);
    await flow.endTimespan();

    console.log('Step 3: Open First Table Product Card');
    await flow.startTimespan({ name: '3. Open First Table Product Card' });
    await clickViaLocator(page, SELECTORS.products.firstProductCard);
    await safeNavigationWait(page);
    await flow.endTimespan();

    console.log('Step 4: Add Table to Cart');
    await flow.startTimespan({ name: '4. Add Table to Cart' });
    await clickViaLocator(page, SELECTORS.products.addToCartBtn);
    await page.waitForNetworkIdle({ idleTime: 500 }).catch(() => {});
    await waitTillHTMLRendered(page);
    await flow.endTimespan();

    console.log('Step 5: Open Cart');
    await flow.startTimespan({ name: '5. Open Cart' });
    await clickViaLocator(page, SELECTORS.menu.cartTab);
    await safeNavigationWait(page);
    await flow.endTimespan();

    console.log('Step 6: Click Place an order');
    await flow.startTimespan({ name: '6. Click Place an order' });
    await clickViaLocator(page, SELECTORS.cart.placeOrderOpenBtn);
    await waitForSelector(page, SELECTORS.cartForm.formContainer);
    await waitTillHTMLRendered(page);
    await flow.endTimespan();

    console.log('Step 7: Fill Form & Place Order');
    await flow.startTimespan({ name: '7. Fill Form & Place Order' });
    await fillCheckoutForm(page, TEST_CUSTOMER);
    await clickViaLocator(page, SELECTORS.cartForm.placeOrderBtn);
    await safeNavigationWait(page);
    await flow.endTimespan();

    const reportHtml = await flow.generateReport();
    fs.writeFileSync(CONFIG.reportName, reportHtml);
    console.log(`Flow report saved to: ${CONFIG.reportName}`);
  } finally {
    await browser.close();
  }
}

runLighthouseFlow().catch(err => {
  console.error(err);
  process.exitCode = 1;
});
