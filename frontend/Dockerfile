# Imagen de producción del frontend: compila con Vite y sirve los archivos estáticos con nginx.
# Construir desde la carpeta frontend/:  docker build -t avicontrol-m3-frontend .

# --- Etapa 1: compilar ---
FROM node:22-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# "/" hace que el navegador llame al mismo sitio que sirve la página; nginx reenvía /api al backend.
# Así no hay que escribir una IP ni configurar CORS. Vite lo incrusta al compilar, no en tiempo de ejecución.
ARG VITE_API_URL=/
ENV VITE_API_URL=$VITE_API_URL
RUN npm run build

# --- Etapa 2: servir ---
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
