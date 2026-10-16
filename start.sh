#!/bin/sh

set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BACKEND_DIR="$ROOT_DIR/uctma"
CONVERTER_DIR="$ROOT_DIR/docling_converter_service"
FRONTEND_DIR="$ROOT_DIR/uctma_frontend"

fail() {
    printf '[HATA] %s\n' "$*" >&2
    exit 1
}

command -v docker >/dev/null 2>&1 || fail "Docker yüklü değil veya PATH içinde bulunamadı."
docker compose version >/dev/null 2>&1 || fail "Docker Compose eklentisi bulunamadı."
docker info >/dev/null 2>&1 || fail "Docker çalışmıyor. Docker Desktop/daemon servisini başlatıp tekrar deneyin."

for required_file in \
    "$BACKEND_DIR/docker-compose.yml" \
    "$BACKEND_DIR/.env.example" \
    "$CONVERTER_DIR/docker-compose.yml" \
    "$CONVERTER_DIR/.env.example" \
    "$FRONTEND_DIR/docker-compose.yml" \
    "$FRONTEND_DIR/.env.example"; do
    [ -f "$required_file" ] || fail "Gerekli proje dosyası bulunamadı: $required_file"
done

ensure_env_file() {
    target=$1
    template=$2

    if [ ! -f "$target" ]; then
        cp "$template" "$target"
        printf '[AYAR] %s dosyası örnek şablondan oluşturuldu.\n' "${target#"$ROOT_DIR"/}"
    fi
}

BACKEND_ENV="$BACKEND_DIR/.env"
CONVERTER_ENV="$CONVERTER_DIR/.env"
FRONTEND_ENV="$FRONTEND_DIR/.env"

ensure_env_file "$BACKEND_ENV" "$BACKEND_DIR/.env.example"
ensure_env_file "$CONVERTER_ENV" "$CONVERTER_DIR/.env.example"
ensure_env_file "$FRONTEND_ENV" "$FRONTEND_DIR/.env.example"

printf '[MODEL] Kalıcı model volume kontrol ediliyor...\n'
docker volume create docling_converter_service_docling_models >/dev/null

if grep -q '^JWT_SECRET=GENERATED_BY_START_SH$' "$BACKEND_ENV" \
    || ! grep -q '^JWT_SECRET=.' "$BACKEND_ENV"; then
    JWT_SECRET=$(od -An -N64 -tx1 /dev/urandom | tr -d ' \n')
    [ -n "$JWT_SECRET" ] || fail "Güvenli JWT anahtarı üretilemedi."
    sed "s/^JWT_SECRET=.*/JWT_SECRET=$JWT_SECRET/" "$BACKEND_ENV" > "$BACKEND_ENV.tmp"
    mv "$BACKEND_ENV.tmp" "$BACKEND_ENV"
    printf '[AYAR] Backend JWT anahtarı üretildi.\n'
fi

if ! grep -q '^BOOTSTRAP_ADMIN_EMAIL=.' "$BACKEND_ENV"; then
    if grep -q '^BOOTSTRAP_ADMIN_EMAIL=' "$BACKEND_ENV"; then
        sed 's/^BOOTSTRAP_ADMIN_EMAIL=.*/BOOTSTRAP_ADMIN_EMAIL=admin@uctma.local/' "$BACKEND_ENV" > "$BACKEND_ENV.tmp"
        mv "$BACKEND_ENV.tmp" "$BACKEND_ENV"
    else
        printf '\nBOOTSTRAP_ADMIN_EMAIL=admin@uctma.local\n' >> "$BACKEND_ENV"
    fi
fi

if grep -q '^BOOTSTRAP_ADMIN_PASSWORD=GENERATED_BY_START_SH$' "$BACKEND_ENV" \
    || ! grep -q '^BOOTSTRAP_ADMIN_PASSWORD=.' "$BACKEND_ENV"; then
    BOOTSTRAP_ADMIN_PASSWORD=$(od -An -N32 -tx1 /dev/urandom | tr -d ' \n')
    [ -n "$BOOTSTRAP_ADMIN_PASSWORD" ] || fail "Güvenli ilk admin parolası üretilemedi."
    if grep -q '^BOOTSTRAP_ADMIN_PASSWORD=' "$BACKEND_ENV"; then
        sed "s/^BOOTSTRAP_ADMIN_PASSWORD=.*/BOOTSTRAP_ADMIN_PASSWORD=$BOOTSTRAP_ADMIN_PASSWORD/" "$BACKEND_ENV" > "$BACKEND_ENV.tmp"
        mv "$BACKEND_ENV.tmp" "$BACKEND_ENV"
    else
        printf 'BOOTSTRAP_ADMIN_PASSWORD=%s\n' "$BOOTSTRAP_ADMIN_PASSWORD" >> "$BACKEND_ENV"
    fi
    printf '[AYAR] İlk admin parolası güvenli şekilde üretildi.\n'
fi
chmod 600 "$BACKEND_ENV"

backend_compose() {
    docker compose --env-file "$BACKEND_ENV" \
        -f "$BACKEND_DIR/docker-compose.yml" \
        --project-directory "$BACKEND_DIR" "$@"
}

converter_compose() {
    docker compose --env-file "$CONVERTER_ENV" \
        -f "$CONVERTER_DIR/docker-compose.yml" \
        --project-directory "$CONVERTER_DIR" "$@"
}

frontend_compose() {
    docker compose --env-file "$FRONTEND_ENV" \
        -f "$FRONTEND_DIR/docker-compose.yml" \
        --project-directory "$FRONTEND_DIR" "$@"
}

printf '[BAŞLAT] PostgreSQL ve RabbitMQ başlatılıyor...\n'
backend_compose up -d postgres-db rabbitmq

printf '[BEKLE] RabbitMQ hazır olması bekleniyor...\n'
attempt=0
until docker exec uctma_rabbitmq rabbitmq-diagnostics -q ping >/dev/null 2>&1; do
    attempt=$((attempt + 1))
    if [ "$attempt" -ge 60 ]; then
        fail "RabbitMQ 120 saniye içinde hazır olmadı. Logları kontrol edin: docker logs uctma_rabbitmq"
    fi
    sleep 2
done

printf '[BAŞLAT] Backend başlatılıyor...\n'
backend_compose up -d --build backend-service

printf '[MODEL] Docling, RapidOCR ve Whisper modelleri kalıcı volume içine indiriliyor...\n'
if ! backend_compose run --rm model-prefetch; then
    printf '[UYARI] Model prefetch başarısız oldu. İlk dönüştürme isteğinde eksik modeller internetten indirilmeye çalışılacak.\n' >&2
fi

printf '[BAŞLAT] Docling converter başlatılıyor...\n'
converter_compose up -d --build docling_converter_service

printf '[BAŞLAT] Frontend başlatılıyor...\n'
frontend_compose up -d --build frontend

printf '\n[HAZIR] UCTMA servisleri başlatıldı.\n'
printf 'Arayüz: http://localhost:3000\n'
printf 'Backend: http://localhost:8080\n'
printf 'Converter: http://localhost:8000\n'
printf 'İlk admin e-posta adresi ve parolası: uctma/.env dosyasında.\n'
printf 'Durdurmak için ilgili Compose dizinlerinde docker compose down çalıştırın.\n'
