# Estágio 1: compila com o Node.
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# Estágio 2: serve os arquivos prontos com o nginx, que também repassa a API ao backend.
FROM nginx:1.27-alpine
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
# Padrões do Docker Compose. A hospedagem troca os dois valores.
ENV BACKEND_URL=http://backend:8080 \
    BACKEND_HOST_HEADER=\$http_host
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
