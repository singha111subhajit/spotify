# Stage 1: Build React frontend
FROM node:18 AS frontend
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm install
COPY frontend/ ./
RUN npm run build

# Stage 2: Build Flask backend and combine
FROM python:3.10-slim AS backend
WORKDIR /app
COPY requirements.txt ./
RUN pip install -r requirements.txt
COPY . ./

# Copy React build output to Flask static and template folders for Flask to serve
RUN mkdir -p static && mkdir -p templates
COPY --from=frontend /app/frontend/build/static ./static/
COPY --from=frontend /app/frontend/build/index.html ./templates/index.html
COPY --from=frontend /app/frontend/build/asset-manifest.json ./static/asset-manifest.json

ENV FLASK_APP=app.py
ENV FLASK_ENV=production

EXPOSE 5600

CMD ["python", "app.py"]
