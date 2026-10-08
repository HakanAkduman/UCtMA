FROM python:3.11-slim

# Sistem bağımlılıklarını yüklüyoruz (PDF ve görsel işleme için gerekli)
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    libgomp1 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Kütüphaneleri kopyalayıp yüklüyoruz
COPY requirements.txt .
RUN pip install --upgrade pip && \
    pip install --no-cache-dir --default-timeout=1000 -r requirements.txt


# Proje kodlarını kopyalıyoruz
COPY main.py .

# Docling modellerinin indirileceği dizini kalıcı yapmak için ortam değişkeni tanımlıyoruz
ENV HF_HOME=/app/models_cache

EXPOSE 8000

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
