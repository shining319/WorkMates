# Build context: the repository root (set in the deploy workflow),
# because nginx.conf lives in deploy/, outside frontend/

# --- build stage: type check and build the SPA ---
FROM node:22-alpine AS build
WORKDIR /app
# Copy the lockfile first so the install layer is cached until dependencies change
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ .
RUN npm run build

# --- run stage: Nginx serves the static files only ---
FROM nginx:stable
COPY deploy/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
