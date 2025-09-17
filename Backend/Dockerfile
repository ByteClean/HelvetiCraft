# Dockerfile for Node.js Backend
FROM node:20-slim
WORKDIR /app
COPY . /app
RUN npm install --production
CMD ["npm run dev"]
