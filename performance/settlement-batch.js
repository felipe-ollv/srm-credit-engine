import http from 'k6/http';
import { check } from 'k6';

const apiBaseUrl = __ENV.API_BASE_URL || 'http://localhost:8080';
const keycloakUrl = __ENV.KEYCLOAK_URL || 'http://localhost:8081';

export const options = {
  scenarios: {
    batch_of_100: {
      executor: 'shared-iterations',
      vus: 1,
      iterations: 1,
      maxDuration: '30s',
    },
  },
  thresholds: {
    http_req_failed: ['rate==0'],
    'http_req_failed{operation:settlement_batch}': ['rate==0'],
    'http_req_duration{operation:settlement_batch}': ['p(95)<=2000'],
  },
};

export function setup() {
  const token = accessToken();
  const document = uniqueCnpj();
  const assignor = jsonRequest(
    'POST',
    '/api/v1/assignors',
    { document, legalName: `Cedente k6 ${document}` },
    token,
    201,
  );
  const receivableIds = [];
  for (let index = 0; index < 100; index += 1) {
    const receivable = jsonRequest(
      'POST',
      '/api/v1/receivables',
      {
        assignorId: assignor.id,
        type: index % 2 === 0 ? 'DUPLICATA_MERCANTIL' : 'CHEQUE_PRE_DATADO',
        faceValue: `${1000 + index}.00`,
        dueDate: futureDate(3),
      },
      token,
      201,
    );
    receivableIds.push(receivable.id);
  }
  return { token, receivableIds };
}

export default function (data) {
  const response = http.post(
    `${apiBaseUrl}/api/v1/settlement-batches`,
    JSON.stringify({
      items: data.receivableIds.map((receivableId) => ({
        receivableId,
        paymentCurrency: 'BRL',
      })),
    }),
    {
      tags: { operation: 'settlement_batch' },
      headers: {
        Authorization: `Bearer ${data.token}`,
        'Content-Type': 'application/json',
        'Idempotency-Key': `k6-batch-${Date.now()}`,
        'X-Correlation-Id': 'k6-batch-100',
      },
    },
  );
  check(response, {
    'batch returns 200': (result) => result.status === 200,
    'all 100 items succeed': (result) => {
      const body = result.json();
      return body.items?.length === 100 && body.items.every((item) => item.status === 'SUCCESS');
    },
  });
}

function jsonRequest(method, path, body, token, expectedStatus) {
  const response = http.request(method, `${apiBaseUrl}${path}`, JSON.stringify(body), {
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  });
  if (!check(response, { [`${path} returns ${expectedStatus}`]: (result) => result.status === expectedStatus })) {
    throw new Error(`${path} failed with status ${response.status}: ${response.body}`);
  }
  return response.json();
}

function accessToken() {
  const response = http.post(
    `${keycloakUrl}/realms/srm-credit-engine/protocol/openid-connect/token`,
    {
      grant_type: 'client_credentials',
      client_id: __ENV.CLIENT_ID || 'srm-credit-engine-load-test',
      client_secret: __ENV.CLIENT_SECRET || 'load-test-secret',
    },
  );
  if (response.status !== 200) {
    throw new Error(`Keycloak authentication failed with status ${response.status}`);
  }
  return response.json('access_token');
}

function uniqueCnpj() {
  const seed = String(Date.now()).slice(-8);
  const base = `43${seed}01`.slice(0, 12);
  const first = checkDigit(base, [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]);
  const withFirst = `${base}${first}`;
  return `${withFirst}${checkDigit(withFirst, [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2])}`;
}

function checkDigit(digits, weights) {
  const total = [...digits].reduce((sum, digit, index) => sum + Number(digit) * weights[index], 0);
  const remainder = total % 11;
  return remainder < 2 ? 0 : 11 - remainder;
}

function futureDate(months) {
  const date = new Date();
  date.setUTCDate(1);
  date.setUTCMonth(date.getUTCMonth() + months);
  return date.toISOString().slice(0, 10);
}
