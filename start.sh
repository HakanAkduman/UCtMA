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

for required_file in \
    "$BACKEND_DIR/docker-compose.yml" \
    "$BACKEND_DIR/.env.example" \
    "$CONVERTER_DIR/docker-compose.yml" \
    "$CONVERTER_DIR/.env.example" \
    "$FRONTEND_DIR/docker-compose.yml" \
    "$FRONTEND_DIR/.env.example"; do
    [ -f "$required_file" ] || fail "Gerekli proje dosyası bulunamadı: $required_file"
done

DOCKER_USE_SUDO=false

docker_cmd() {
    if [ "$DOCKER_USE_SUDO" = true ]; then
        sudo docker "$@"
    else
        docker "$@"
    fi
}

run_as_root() {
    if [ "$(id -u)" -eq 0 ]; then
        "$@"
    else
        command -v sudo >/dev/null 2>&1 || fail "Kurulum için sudo gerekli."
        sudo "$@"
    fi
}

confirm_docker_install() {
    printf '[KURULUM] Docker Engine ve Docker Compose kurulumu için yönetici izni gerekecek. Devam edilsin mi? [e/H] ' >&2
    [ -t 0 ] || fail "Etkileşimsiz kullanımda Docker otomatik kurulamaz; Docker/Compose'u kurup ./start.sh komutunu tekrar çalıştırın."
    IFS= read -r answer || fail "Kurulum onayı okunamadı."
    case "$answer" in
        e|E|evet|EVET|y|Y|yes|YES) ;;
        *) fail "Docker kurulumu kullanıcı tarafından iptal edildi." ;;
    esac
}

install_docker_linux() {
    [ -r /etc/os-release ] || fail "Linux dağıtımı tanınamadı; Docker'ı elle kurup tekrar deneyin."
    # shellcheck disable=SC1091
    . /etc/os-release
    case "${ID:-}" in
        ubuntu|debian) ;;
        *) fail "Otomatik Docker kurulumu yalnızca Ubuntu/Debian destekler. Diğer dağıtımlarda Docker Engine ve Compose eklentisini elle kurun." ;;
    esac

    case "$(uname -m)" in
        x86_64) architecture=amd64 ;;
        aarch64|arm64) architecture=arm64 ;;
        armv7l) architecture=armhf ;;
        *) fail "Docker için desteklenmeyen CPU mimarisi: $(uname -m)" ;;
    esac

    codename=${VERSION_CODENAME:-${UBUNTU_CODENAME:-}}
    [ -n "$codename" ] || fail "Ubuntu/Debian sürüm kod adı bulunamadı; Docker'ı elle kurup tekrar deneyin."
    confirm_docker_install

    printf '[KURULUM] Docker resmi deposu hazırlanıyor...\n'
    run_as_root apt-get update
    run_as_root apt-get install -y ca-certificates curl
    run_as_root install -m 0755 -d /etc/apt/keyrings
    key_file=$(mktemp)
    if ! curl -fsSL "https://download.docker.com/linux/$ID/gpg" -o "$key_file"; then
        rm -f "$key_file"
        fail "Docker imza anahtarı indirilemedi."
    fi
    run_as_root install -m 0644 "$key_file" /etc/apt/keyrings/docker.asc
    rm -f "$key_file"
    printf 'Types: deb\nURIs: https://download.docker.com/linux/%s\nSuites: %s\nComponents: stable\nArchitectures: %s\nSigned-By: /etc/apt/keyrings/docker.asc\n' \
        "$ID" "$codename" "$architecture" \
        | run_as_root tee /etc/apt/sources.list.d/docker.sources >/dev/null
    run_as_root apt-get update
    run_as_root apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
}

install_docker_macos() {
    [ "$(uname -s)" = Darwin ] || return 1
    command -v brew >/dev/null 2>&1 \
        || fail "macOS için Homebrew ve Docker Desktop gereklidir. Homebrew'u kurup tekrar deneyin."
    confirm_docker_install
    brew install --cask docker
    [ -d /Applications/Docker.app ] || [ -d "$HOME/Applications/Docker.app" ] \
        || fail "Docker Desktop kurulumu tamamlandı ancak uygulama bulunamadı."
    open -a Docker || fail "Docker Desktop kuruldu ancak başlatılamadı; Docker uygulamasını açıp tekrar deneyin."
    wait_for_docker_macos
}

start_docker_macos() {
    if [ ! -d /Applications/Docker.app ] && [ ! -d "$HOME/Applications/Docker.app" ]; then
        install_docker_macos
        return
    fi
    open -a Docker || fail "Docker Desktop başlatılamadı; Docker uygulamasını açıp tekrar deneyin."
    wait_for_docker_macos
}

wait_for_docker_macos() {
    attempt=0
    until docker info >/dev/null 2>&1; do
        attempt=$((attempt + 1))
        if [ "$attempt" -ge 60 ]; then
            fail "Docker Desktop başlatılması 120 saniyeyi aştı. İlk kullanım onaylarını tamamlayıp tekrar deneyin."
        fi
        sleep 2
    done
}

start_docker_daemon() {
    if command -v systemctl >/dev/null 2>&1; then
        run_as_root systemctl enable --now docker
    elif command -v service >/dev/null 2>&1; then
        run_as_root service docker start
    else
        return 1
    fi
}

ensure_docker() {
    if ! command -v docker >/dev/null 2>&1 || ! docker compose version >/dev/null 2>&1; then
        printf '[KONTROL] Docker Engine veya Compose eklentisi bulunamadı.\n'
        case "$(uname -s)" in
            Linux) install_docker_linux ;;
            Darwin) install_docker_macos ;;
            *) fail "Bu işletim sisteminde otomatik Docker kurulumu desteklenmiyor; Docker Engine ve Compose'u kurun." ;;
        esac
    fi

    if docker info >/dev/null 2>&1; then
        DOCKER_USE_SUDO=false
    elif command -v sudo >/dev/null 2>&1 && sudo docker info >/dev/null 2>&1; then
        DOCKER_USE_SUDO=true
    else
        printf '[KONTROL] Docker servisi başlatılmaya çalışılıyor...\n'
        if [ "$(uname -s)" = Darwin ]; then
            start_docker_macos
            DOCKER_USE_SUDO=false
        else
            if ! start_docker_daemon; then
                if command -v sudo >/dev/null 2>&1 && sudo docker info >/dev/null 2>&1; then
                    DOCKER_USE_SUDO=true
                else
                    fail "Docker daemon başlatılamadı. Servis durumunu veya Docker Desktop'ı kontrol edip tekrar deneyin."
                fi
            elif docker info >/dev/null 2>&1; then
                DOCKER_USE_SUDO=false
            elif command -v sudo >/dev/null 2>&1 && sudo docker info >/dev/null 2>&1; then
                DOCKER_USE_SUDO=true
            else
                fail "Docker daemon çalışmıyor veya kullanıcı Docker'a erişemiyor."
            fi
        fi
    fi

    docker_cmd compose version >/dev/null 2>&1 \
        || fail "Docker Compose eklentisi kurulu değil. Docker/Compose kurulumunu tamamlayıp tekrar deneyin."
    docker_cmd info >/dev/null 2>&1 \
        || fail "Docker çalışmıyor. Docker Desktop/daemon servisini başlatıp tekrar deneyin."
}

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

set_env_value() {
    target=$1
    key=$2
    value=$3
    if grep -q "^$key=" "$target"; then
        sed "s|^$key=.*|$key=$value|" "$target" > "$target.tmp"
        mv "$target.tmp" "$target"
    else
        printf '%s=%s\n' "$key" "$value" >> "$target"
    fi
}

ensure_docker

printf '[MODEL] Kalıcı model volume kontrol ediliyor...\n'
docker_cmd volume create docling_converter_service_docling_models >/dev/null

if grep -q '^DB_PASSWORD=change_this_database_password$' "$BACKEND_ENV" \
    || ! grep -q '^DB_PASSWORD=.' "$BACKEND_ENV"; then
    DB_PASSWORD=$(od -An -N32 -tx1 /dev/urandom | tr -d ' \n')
    [ -n "$DB_PASSWORD" ] || fail "Güvenli veritabanı parolası üretilemedi."
    set_env_value "$BACKEND_ENV" "DB_PASSWORD" "$DB_PASSWORD"
    printf '[AYAR] Veritabanı parolası güvenli şekilde üretildi.\n'
fi
DB_PASSWORD=$(sed -n 's/^DB_PASSWORD=//p' "$BACKEND_ENV" | head -n 1)
[ -n "$DB_PASSWORD" ] || fail "uctma/.env içinde DB_PASSWORD tanımlı olmalıdır."

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

CALLBACK_SHARED_SECRET=$(sed -n 's/^CALLBACK_SHARED_SECRET=//p' "$BACKEND_ENV" | head -n 1)
if [ -z "$CALLBACK_SHARED_SECRET" ] || [ "$CALLBACK_SHARED_SECRET" = "GENERATED_BY_START_SH" ]; then
    CALLBACK_SHARED_SECRET=$(sed -n 's/^CALLBACK_SHARED_SECRET=//p' "$CONVERTER_ENV" | head -n 1)
fi
if [ -z "$CALLBACK_SHARED_SECRET" ] || [ "$CALLBACK_SHARED_SECRET" = "GENERATED_BY_START_SH" ]; then
    CALLBACK_SHARED_SECRET=$(od -An -N32 -tx1 /dev/urandom | tr -d ' \n')
fi
[ -n "$CALLBACK_SHARED_SECRET" ] || fail "Callback için güvenli paylaşılan anahtar üretilemedi."
[ "${#CALLBACK_SHARED_SECRET}" -ge 32 ] || fail "CALLBACK_SHARED_SECRET en az 32 karakter olmalıdır."

set_env_value "$BACKEND_ENV" "CALLBACK_SHARED_SECRET" "$CALLBACK_SHARED_SECRET"
set_env_value "$CONVERTER_ENV" "CALLBACK_SHARED_SECRET" "$CALLBACK_SHARED_SECRET"
chmod 600 "$BACKEND_ENV"
chmod 600 "$CONVERTER_ENV"

DB_NAME=$(sed -n 's/^DB_NAME=//p' "$BACKEND_ENV" | head -n 1)
DB_USERNAME=$(sed -n 's/^DB_USERNAME=//p' "$BACKEND_ENV" | head -n 1)
[ -n "$DB_NAME" ] || fail "uctma/.env içinde DB_NAME tanımlı olmalıdır."
[ -n "$DB_USERNAME" ] || fail "uctma/.env içinde DB_USERNAME tanımlı olmalıdır."

backend_compose() {
    docker_cmd compose --env-file "$BACKEND_ENV" \
        -f "$BACKEND_DIR/docker-compose.yml" \
        --project-directory "$BACKEND_DIR" "$@"
}

converter_compose() {
    docker_cmd compose --env-file "$CONVERTER_ENV" \
        -f "$CONVERTER_DIR/docker-compose.yml" \
        --project-directory "$CONVERTER_DIR" "$@"
}

frontend_compose() {
    docker_cmd compose --env-file "$FRONTEND_ENV" \
        -f "$FRONTEND_DIR/docker-compose.yml" \
        --project-directory "$FRONTEND_DIR" "$@"
}

printf '[BAŞLAT] PostgreSQL ve RabbitMQ başlatılıyor...\n'
backend_compose up -d postgres-db rabbitmq

printf '[AYAR] PostgreSQL kullanıcısının parolası yapılandırmayla eşleştiriliyor...\n'
attempt=0
until docker_cmd exec rag-platform-postgres pg_isready -U "$DB_USERNAME" -d "$DB_NAME" >/dev/null 2>&1; do
    attempt=$((attempt + 1))
    if [ "$attempt" -ge 60 ]; then
        fail "PostgreSQL 120 saniye içinde hazır olmadı. Veritabanı loglarını kontrol edin: docker logs rag-platform-postgres"
    fi
    sleep 2
done
SQL_ROLE=$(printf '%s' "$DB_USERNAME" | sed 's/"/""/g')
SQL_PASSWORD=$(printf '%s' "$DB_PASSWORD" | sed "s/'/''/g")
if ! printf 'ALTER ROLE "%s" WITH PASSWORD '\''%s'\'';\n' "$SQL_ROLE" "$SQL_PASSWORD" \
    | docker_cmd exec -i rag-platform-postgres psql -v ON_ERROR_STOP=1 \
        -U "$DB_USERNAME" -d "$DB_NAME" >/dev/null; then
    fail "PostgreSQL kullanıcısının parolası eşleştirilemedi. Veritabanı verisi korunmuştur; uctma/.env içindeki DB_NAME/DB_USERNAME değerlerini mevcut volume ile eşleştirip tekrar deneyin."
fi

printf '[BEKLE] RabbitMQ hazır olması bekleniyor...\n'
attempt=0
until docker_cmd exec uctma_rabbitmq rabbitmq-diagnostics -q ping >/dev/null 2>&1; do
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
