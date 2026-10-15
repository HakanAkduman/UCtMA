FROM python:3.11-slim

# Sistem bağımlılıkları (PDF, görsel ve OCR işleme için gerekli)
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    ca-certificates \
    libgomp1 \
    libpq-dev \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Kütüphaneleri kopyalayıp yüklüyoruz
COPY requirements.txt .
RUN pip install --upgrade pip && \
    pip install --no-cache-dir --default-timeout=1000 -r requirements.txt

# Tüm proje kodlarını (main.py, docling.py, consumer.py vb.) kopyalıyoruz
COPY . .

# Shared Volume için gerekli dizinleri oluşturuyoruz
RUN mkdir -p /app/uploads/chunks
RUN mkdir -p /app/models_cache

# Docling / HuggingFace modellerinin indirileceği dizin
ENV HF_HOME=/app/models_cache
ENV REQUESTS_CA_BUNDLE=/etc/ssl/certs/ca-certificates.crt

# HTTP API and RabbitMQ consumer run in the same service container.
CMD ["sh", "-c", "uvicorn main:app --host 0.0.0.0 --port 8000 & exec python consumer.py"]