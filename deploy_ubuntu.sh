#!/usr/bin/env bash
set -euo pipefail

# Manga Chapter Date Fixer - Ubuntu deployment script
# - Installs OpenJDK 17, git, curl
# - Clones and builds the project from GitHub
# - Installs a systemd service running the web API on port 1996
# - Sets CONFIG_PATH via environment file
# - Optionally opens UFW port 1996
#
# Usage:
#   sudo ./deploy_ubuntu.sh [--repo URL] [--branch BRANCH] [--config /path/to/config.yaml] [--no-ufw]
#
# Defaults:
#   --repo   https://github.com/mrunknownpbu/manga-chapter-date-fixer.git
#   --branch main
#   --config /etc/manga-chapter-date-fixer.yaml
#

REPO_URL="https://github.com/mrunknownpbu/manga-chapter-date-fixer.git"
BRANCH="main"
CONFIG_DST="/etc/manga-chapter-date-fixer.yaml"
OPEN_UFW=true

APP_USER="manga-cdf"
APP_GROUP="manga-cdf"
APP_DIR="/opt/manga-chapter-date-fixer"
SERVICE_NAME="manga-chapter-date-fixer"
ENV_FILE="/etc/${SERVICE_NAME}.env"
SERVICE_FILE="/etc/systemd/system/${SERVICE_NAME}.service"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --repo)
      REPO_URL="$2"; shift 2;;
    --branch)
      BRANCH="$2"; shift 2;;
    --config)
      CONFIG_DST="$2"; shift 2;;
    --no-ufw)
      OPEN_UFW=false; shift 1;;
    -h|--help)
      echo "Usage: sudo ./deploy_ubuntu.sh [--repo URL] [--branch BRANCH] [--config /path/to/config.yaml] [--no-ufw]"; exit 0;;
    *)
      echo "Unknown arg: $1"; exit 1;;
  esac
done

if [[ $(id -u) -ne 0 ]]; then
  echo "Please run as root (e.g., sudo $0)" >&2
  exit 1
fi

export DEBIAN_FRONTEND=noninteractive

echo "[1/8] Installing dependencies..."
apt-get update -y
apt-get install -y --no-install-recommends openjdk-17-jdk-headless git curl ca-certificates

JAVA_BIN="$(command -v java || true)"
if [[ -z "$JAVA_BIN" ]]; then
  echo "Java not found after install" >&2
  exit 1
fi

if ! id "$APP_USER" &>/dev/null; then
  echo "[2/8] Creating service user $APP_USER..."
  useradd --system --home "$APP_DIR" --shell /usr/sbin/nologin "$APP_USER"
fi

mkdir -p "$APP_DIR"
chown -R "$APP_USER":"$APP_GROUP" "$APP_DIR" 2>/dev/null || true
# Ensure group exists
if ! getent group "$APP_GROUP" >/dev/null; then
  groupadd --system "$APP_GROUP"
  usermod -g "$APP_GROUP" "$APP_USER"
fi

WORK_DIR="/tmp/${SERVICE_NAME}-build-$$"
trap 'rm -rf "$WORK_DIR"' EXIT
mkdir -p "$WORK_DIR"

echo "[3/8] Cloning repository $REPO_URL (branch $BRANCH)..."
GIT_SSH_COMMAND="ssh -o StrictHostKeyChecking=no" git clone --depth 1 --branch "$BRANCH" "$REPO_URL" "$WORK_DIR/repo"

echo "[4/8] Building application (Gradle wrapper)..."
cd "$WORK_DIR/repo"
./gradlew clean build -x test

JAR_SRC="$(ls -1 build/libs/manga-chapter-date-fixer.jar 2>/dev/null || true)"
if [[ -z "$JAR_SRC" ]]; then
  echo "Build artifact not found in build/libs" >&2
  exit 1
fi

install -d -o "$APP_USER" -g "$APP_GROUP" "$APP_DIR"
install -m 0644 -o "$APP_USER" -g "$APP_GROUP" "$JAR_SRC" "$APP_DIR/app.jar"

# Install default config if missing
if [[ ! -f "$CONFIG_DST" ]]; then
  echo "[5/8] Installing default config to $CONFIG_DST"
  install -m 0644 chapterReleaseDateProviders.yaml "$CONFIG_DST"
fi

# Environment file
cat > "$ENV_FILE" <<EOF
# Environment for ${SERVICE_NAME}
CONFIG_PATH=${CONFIG_DST}
# JAVA_OPTS can be used to tune memory, e.g. -Xms128m -Xmx256m
JAVA_OPTS=
EOF
chmod 0644 "$ENV_FILE"

# Systemd service
echo "[6/8] Installing systemd service ${SERVICE_NAME}.service"
cat > "$SERVICE_FILE" <<EOF
[Unit]
Description=Manga Chapter Date Fixer API
After=network.target

[Service]
Type=simple
User=${APP_USER}
Group=${APP_GROUP}
WorkingDirectory=${APP_DIR}
EnvironmentFile=${ENV_FILE}
ExecStart=/usr/bin/java \$JAVA_OPTS -jar ${APP_DIR}/app.jar
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF
chmod 0644 "$SERVICE_FILE"

systemctl daemon-reload
systemctl enable --now "$SERVICE_NAME"

# UFW open port if active
if $OPEN_UFW && command -v ufw >/dev/null 2>&1; then
  if ufw status | grep -q "Status: active"; then
    echo "[7/8] Opening UFW port 1996/tcp"
    ufw allow 1996/tcp || true
  fi
fi

echo "[8/8] Verifying service health..."
sleep 2
STATUS=$(systemctl is-active "$SERVICE_NAME" || true)
echo "Service status: $STATUS"
if [[ "$STATUS" != "active" ]]; then
  journalctl -u "$SERVICE_NAME" --no-pager -n 50 || true
  echo "Service is not active; see logs above." >&2
  exit 1
fi

# Try health endpoint
set +e
curl -fsS "http://localhost:1996/health" && echo || echo "Health endpoint not reachable yet"
set -e

echo "Deployment completed. Service: ${SERVICE_NAME} listening on port 1996"
echo "Config: ${CONFIG_DST} (edit and restart with: sudo systemctl restart ${SERVICE_NAME})"