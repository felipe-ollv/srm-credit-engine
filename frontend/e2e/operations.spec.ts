import { expect, Page, test } from '@playwright/test';

const operator = {
  username: process.env['E2E_OPERATOR_USERNAME'] ?? 'operator',
  password: process.env['E2E_OPERATOR_PASSWORD'] ?? 'operator123',
};
const admin = {
  username: process.env['E2E_ADMIN_USERNAME'] ?? 'admin',
  password: process.env['E2E_ADMIN_PASSWORD'] ?? 'admin123',
};

test.describe('operational workflow', () => {
  test.skip(({ isMobile }) => isMobile, 'The complete workflow runs once on desktop');

  test('registers an assignor and receivable, settles it and finds the statement', async ({ page }) => {
    await login(page, operator.username, operator.password);
    const suffix = String(Date.now()).slice(-8);
    const legalName = `Cedente E2E ${suffix}`;
    const document = createCnpj(`43${suffix}01`.slice(0, 12));
    const faceValue = `${Number(suffix.slice(-4)) + 1000},37`;
    const faceDisplay = new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 2 })
      .format(Number(faceValue.replace(',', '.')));

    await page.getByRole('link', { name: 'Cedentes' }).click();
    await page.getByRole('textbox', { name: 'CNPJ', exact: true }).fill(document);
    await page.getByRole('textbox', { name: 'Razão social', exact: true }).fill(legalName);
    await page.getByRole('button', { name: 'Cadastrar', exact: true }).click();
    await expect(page.getByText('Cedente cadastrado com sucesso.')).toBeVisible();

    await page.getByRole('link', { name: 'Recebíveis' }).click();
    await page.getByRole('combobox', { name: 'Cedente' }).first().click();
    await page.getByRole('option', { name: new RegExp(legalName) }).click();
    await page.getByRole('textbox', { name: 'Valor de face' }).fill(faceValue);
    await page.getByLabel('Vencimento').fill(futureDueDate(2));
    await page.getByRole('button', { name: 'Cadastrar recebível' }).click();
    await expect(page.getByText('Recebível cadastrado com sucesso.')).toBeVisible();

    await page.getByRole('link', { name: 'Liquidação' }).click();
    const row = page.locator('tbody tr').filter({ hasText: faceDisplay });
    await expect(row).toBeVisible();
    await row.getByRole('checkbox').check();
    await page.getByRole('button', { name: 'Liquidar 1 item(ns)' }).click();
    await expect(page.getByText('Sucesso', { exact: true })).toBeVisible();

    await page.getByRole('link', { name: 'Extrato' }).click();
    await page.getByRole('combobox', { name: 'Cedente' }).click();
    await page.getByRole('option', { name: legalName }).click();
    await page.getByRole('button', { name: 'Aplicar filtros' }).click();
    await expect(page.locator('tbody')).toContainText(legalName);
  });

  test('ADMIN can refresh the current exchange rate', async ({ page }) => {
    await login(page, admin.username, admin.password);
    await page.getByRole('link', { name: 'Câmbio' }).click();
    await expect(page.getByRole('heading', { name: 'Administração cambial' })).toBeVisible();
    await page.getByRole('button', { name: 'Atualizar cotação' }).click();
    await expect(page.getByText(/1 USD = R\$/)).toBeVisible();
  });
});

async function login(page: Page, username: string, password: string): Promise<void> {
  await page.goto('/');
  await page.locator('#username').fill(username);
  await page.locator('#password').fill(password);
  await page.locator('#kc-login').click();
  await expect(page.getByRole('heading', { name: 'Simular antecipação' })).toBeVisible();
}

function createCnpj(base: string): string {
  const first = checkDigit(base, [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]);
  const digits = `${base}${first}`;
  return `${digits}${checkDigit(digits, [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2])}`;
}

function checkDigit(digits: string, weights: readonly number[]): number {
  const total = [...digits].reduce((sum, digit, index) => sum + Number(digit) * weights[index], 0);
  const remainder = total % 11;
  return remainder < 2 ? 0 : 11 - remainder;
}

function futureDueDate(months: number): string {
  const now = new Date();
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: 'America/Sao_Paulo', year: 'numeric', month: '2-digit', day: '2-digit',
  }).formatToParts(now);
  const values = Object.fromEntries(parts.map((part) => [part.type, Number(part.value)]));
  const target = new Date(Date.UTC(values['year'], values['month'] - 1 + months, values['day']));
  return target.toISOString().slice(0, 10);
}
