FROM node:20-alpine

WORKDIR /app
ARG FRONTEND_PORT=3000
COPY package*.json ./
RUN npm install

COPY . .
RUN npm run build

EXPOSE ${FRONTEND_PORT}
CMD ["npm", "start"]
