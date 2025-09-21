# Production Dockerfile for Node.js Backend
FROM node:20-slim
WORKDIR /app

# Install only production dependencies
COPY package.json package-lock.json* ./
RUN npm ci --production || npm install --production

# Copy source after installing dependencies to keep layers cacheable
COPY . /app

ENV NODE_ENV=production

EXPOSE 3000
CMD ["node", "src/server.js"]
