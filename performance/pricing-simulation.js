import http from 'k6/http';
import { check } from 'k6';

const apiBaseUrl = __ENV.API_BASE_URL || 'http://localhost:8080';
const keycloakUrl = __ENV.KEYCLOAK_URL || 'http://localhost:8081';

export const options = {
  scenarios: {
    pricing: {
      executor: 'constant-arrival-rate',
      rate: Number(__ENV.RATE || 50),
      timeUnit: '1s',
      duration: __ENV.DURATION || '30s',
      startTime: '1s',
      preAllocatedVUs: 20,
      maxVUs: 50,
      gracefulStop: '5s',
    },
  },
  thresholds: {
    'http_req_failed{operation:pricing}': ['rate==0'],
    'http_req_duration{operation:pricing}': ['p(95)<=100'],
    dropped_iterations: ['count==0'],
  },
};

export function setup() {
  return { token: accessToken() };
}

export default function (data) {
  const response = http.post(
    `${apiBaseUrl}/api/v1/pricing/simulations`,
    JSON.stringify({
      receivableType: 'DUPLICATA_MERCANTIL',
      faceValue: '100000.00',
      dueDate: futureDate(3),
      paymentCurrency: 'BRL',
    }),
    requestParams(data.token),
  );
  check(response, { 'simulation returns 200': (result) => result.status === 200 });
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
  check(response, { 'load-test client authenticated': (result) => result.status === 200 });
  if (response.status !== 200) {
    throw new Error(`Keycloak authentication failed with status ${response.status}`);
  }
  return response.json('access_token');
}

function requestParams(token) {
  return {
    tags: { operation: 'pricing' },
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
      'X-Correlation-Id': `k6-${__VU}-${__ITER}`,
    },
  };
}

function futureDate(months) {
  const date = new Date();
  date.setUTCDate(1);
  date.setUTCMonth(date.getUTCMonth() + months);
  return date.toISOString().slice(0, 10);
}
