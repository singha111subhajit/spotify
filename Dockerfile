FROM python:3.10-slim

WORKDIR /app

# System dependencies (e.g., for psycopg2)
RUN apt-get update && apt-get install -y --no-install-recommends gcc libpq-dev && rm -rf /var/lib/apt/lists/*

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

# Copy backend sources (Flask app, templates, static, etc.)
COPY . .

ENV FLASK_APP=app.py
ENV FLASK_ENV=production
# Placeholder - override at deploy time
ENV DATABASE_URL=postgresql://postgres:postgres@postgres:5432/music_app

EXPOSE 5600

CMD ["gunicorn", "-w", "4", "-b", "0.0.0.0:5600", "app:app"]
