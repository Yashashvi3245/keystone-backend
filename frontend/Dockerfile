# =====================================================================
# KEYSTONE Frontend — Multi-stage Dockerfile
#
# Stage 1 (builder):  Node 22 Alpine — installs deps and runs vite build
# Stage 2 (runtime):  nginx 1.27 Alpine — serves dist/ as static files
#                     and reverse-proxies /api/* to the backend service
# =====================================================================

# ── Stage 1: build ───────────────────────────────────────────────────
FROM node:22-alpine AS builder

WORKDIR /app

# Copy lockfile + manifest first for better layer caching
COPY package.json package-lock.json ./
RUN npm ci --silent

# Copy source and compile
COPY . .
RUN npm run build

# ── Stage 2: serve ───────────────────────────────────────────────────
FROM nginx:1.27-alpine AS runtime

# Remove the default welcome-page config
RUN rm /etc/nginx/conf.d/default.conf

# Inject our nginx config (SPA fallback + /api proxy)
COPY nginx.conf /etc/nginx/conf.d/keystone.conf

# Copy compiled assets
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
