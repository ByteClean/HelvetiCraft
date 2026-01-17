FROM node:20-slim
WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm ci --production || npm install --production

COPY . /app

ENV NODE_ENV=production
EXPOSE 3002
CMD ["node", "src/server.js"]
