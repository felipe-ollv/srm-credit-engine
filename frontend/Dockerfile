FROM node:24-alpine AS build
WORKDIR /workspace
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.29-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist/srm-credit-engine-web/browser /usr/share/nginx/html
ENV API_BASE_URL=http://localhost:8080 \
    KEYCLOAK_URL=http://localhost:8081 \
    KEYCLOAK_REALM=srm-credit-engine \
    KEYCLOAK_CLIENT_ID=srm-credit-engine-web
EXPOSE 8080
CMD ["/bin/sh", "-c", "envsubst < /usr/share/nginx/html/config.template.json > /usr/share/nginx/html/config.json && exec nginx -g 'daemon off;'"]
