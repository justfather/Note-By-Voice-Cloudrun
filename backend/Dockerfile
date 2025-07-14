FROM node:18-alpine

WORKDIR /app

# Copy package files
COPY package*.json ./
COPY tsconfig.json ./

# Copy source code first (needed for build)
COPY src ./src

# Install dependencies and build
RUN npm ci

# Expose port
EXPOSE 8080

# Start the application
CMD ["npm", "start"]