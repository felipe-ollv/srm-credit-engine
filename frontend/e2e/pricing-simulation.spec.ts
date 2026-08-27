import { expect, Page, test } from '@playwright/test';

const credentials = {
  operator: {
    username: process.env['E2E_OPERATOR_USERNAME'] ?? 'operator',
    password: process.env['E2E_OPERATOR_PASSWORD'] ?? 'operator123',
  },
  admin: {
    username: process.env['E2E_ADMIN_USERNAME'] ?? 'admin',
    password: process.env['E2E_ADMIN_PASSWORD'] ?? 'admin123',
  },
  viewer: {
    username: process.env['E2E_VIEWER_USERNAME'] ?? 'viewer',
    password: process.env['E2E_VIEWER_PASSWORD'] ?? 'viewer123',
  },
};

test.describe('pricing golden cases', () => {
  test.skip(({ isMobile }) => isMobile, 'Golden cases run once in the desktop project');

  test.beforeEach(async ({ page }) => {
    await login(page, credentials.operator.username, credentials.operator.password);
  });

  test('C1 — duplicata paga em BRL', async ({ page }) => {
    await simulate(page, {
      type: 'Duplicata Mercantil',
      faceValue: '100.000,00',
      dueDate: futureDueDate(3),
      currency: 'BRL — Real brasileiro',
    });

    await expect(page.locator('.payment-highlight strong')).toHaveText('R$ 92.859,94');
    await expect(page.locator('.financial-summary')).toContainText('R$ 7.140,06');
    await expect(page.getByText('Cotação aplicada')).toHaveCount(0);
  });

  test('C2 — cheque pré-datado pago em BRL', async ({ page }) => {
    await simulate(page, {
      type: 'Cheque Pré-datado',
      faceValue: '25.000,00',
      dueDate: futureDueDate(2),
      currency: 'BRL — Real brasileiro',
    });

    await expect(page.locator('.payment-highlight strong')).toHaveText('R$ 23.337,77');
    await expect(page.locator('.financial-summary')).toContainText('R$ 1.662,23');
  });

  test('C3 — duplicata paga em USD', async ({ page }) => {
    await simulate(page, {
      type: 'Duplicata Mercantil',
      faceValue: '100.000,00',
      dueDate: futureDueDate(3),
      currency: 'USD — Dólar americano',
    });

    await expect(page.locator('.payment-highlight strong')).toHaveText('US$ 17.094,67');
    await expect(page.locator('.financial-summary')).toContainText('R$ 7.140,06');
    await expect(page.locator('.fx-card')).toContainText('1 USD = R$ 5,4321');
  });
});

test.describe('authentication and authorization', () => {
  test.skip(({ isMobile }) => isMobile, 'Authorization runs once in the desktop project');

  test('ADMIN can access the simulator and log out', async ({ page }) => {
    await login(page, credentials.admin.username, credentials.admin.password);
    await expect(page.getByText('ADMIN', { exact: true })).toBeVisible();

    await page.getByRole('button', { name: 'Sair' }).click();
    await expect(page).toHaveURL(/localhost:8081/);
  });

  test('user without an application role receives an explanatory screen', async ({ page }) => {
    await login(page, credentials.viewer.username, credentials.viewer.password, false);

    await expect(page.getByRole('heading', {
      name: 'Seu usuário não possui um perfil operacional',
    })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Sair e trocar de usuário' })).toBeVisible();
  });
});

test('mobile layout stacks the form and result without horizontal overflow', async ({ page, isMobile }) => {
  test.skip(!isMobile, 'Responsive assertion only runs in the mobile project');
  await login(page, credentials.operator.username, credentials.operator.password);

  const formPanel = page.locator('.form-panel');
  const resultPanel = page.locator('.result-panel');
  const formBox = await formPanel.boundingBox();
  const resultBox = await resultPanel.boundingBox();

  expect(formBox).not.toBeNull();
  expect(resultBox).not.toBeNull();
  expect(resultBox!.y).toBeGreaterThan(formBox!.y + formBox!.height - 2);
  const hasHorizontalOverflow = await page.evaluate(
    () => document.documentElement.scrollWidth > document.documentElement.clientWidth,
  );
  expect(hasHorizontalOverflow).toBe(false);
});

async function login(
  page: Page,
  username: string,
  password: string,
  expectSimulator = true,
): Promise<void> {
  await page.goto('/');
  await page.locator('#username').fill(username);
  await page.locator('#password').fill(password);
  await page.locator('#kc-login').click();
  if (expectSimulator) {
    await expect(page.getByRole('heading', { name: 'Simular antecipação' })).toBeVisible();
  }
}

async function simulate(
  page: Page,
  input: {
    type: string;
    faceValue: string;
    dueDate: string;
    currency: string;
  },
): Promise<void> {
  await page.getByRole('combobox', { name: 'Tipo de recebível' }).click();
  await page.getByRole('option', { name: input.type }).click();
  await page.getByRole('textbox', { name: 'Valor de face' }).fill(input.faceValue);
  await page.getByLabel('Data de vencimento').fill(input.dueDate);
  await page.getByRole('combobox', { name: 'Moeda de pagamento' }).click();
  await page.getByRole('option', { name: input.currency }).click();
  await expect(page.getByText('Valor líquido a receber')).toBeVisible();
}

function futureDueDate(months: number): string {
  const today = saoPauloDate();
  const [year, month, day] = today.split('-').map((part) => Number.parseInt(part, 10));
  const zeroBasedTarget = month - 1 + months;
  const targetYear = year + Math.floor(zeroBasedTarget / 12);
  const targetMonth = zeroBasedTarget % 12;
  const maximumDay = new Date(Date.UTC(targetYear, targetMonth + 1, 0)).getUTCDate();
  return isoDate(targetYear, targetMonth + 1, Math.min(day, maximumDay));
}

function saoPauloDate(): string {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: 'America/Sao_Paulo',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(new Date());
  const values = Object.fromEntries(parts.map((part) => [part.type, part.value]));
  return `${values['year']}-${values['month']}-${values['day']}`;
}

function isoDate(year: number, month: number, day: number): string {
  return `${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}`;
}
