#!/usr/bin/env bash
#
# Hokeka Edge Engine — cross-distro Linux installer.
#
# Installs every prerequisite (container runtime OR native Java + Aerospike), brings up the edge and
# its feature store, secures the database so ONLY the edge app can reach it, and generates the TLS
# keystore the transaction API terminates on (the edge refuses to start without one).
#
#   Default (recommended): containerised, distro-agnostic, DB isolated on a private network.
#     sudo ./install.sh --pspid acme --edgeid acme-eu-1 --controlplane https://edge.hokeka.com \
#          --enrollment-code ABCD-1234
#
#   Native (no containers): installs Temurin JRE 25 + Aerospike CE, binds the DB to 127.0.0.1,
#   firewalls port 3000, installs a systemd service.
#     sudo ./install.sh --native --pspid acme --edgeid acme-eu-1
#
set -euo pipefail

# ── defaults ─────────────────────────────────────────────────────────────────────────────────────
MODE="container"
EDGE_IMAGE="registry.hokeka.com/hokeka/edge-engine:0.1.0"
BIND="127.0.0.1"           # host interface the transaction API binds to
PSPID="acme"
EDGEID="acme-1"
CONTROLPLANE="https://edge.hokeka.com"
DATA_DIR="/opt/hokeka"
ALLOW_SUBNET=""            # e.g. 10.0.0.0/24 — restrict the 8443 API to your PSP subnet
ENROLL_CODE=""             # operator-supplied, single-use, 24h TTL — needed for first-boot activation
TLS_CLIENT_AUTH="none"     # none | want | need (mutual TLS from your PSP API nodes)
TLS_PASS=""                # filled by ensure_tls_cert
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

log()  { printf '\033[0;36m[hokeka]\033[0m %s\n' "$*"; }
ok()   { printf '\033[0;32m  ✔\033[0m %s\n' "$*"; }
warn() { printf '\033[0;33m  ! \033[0m%s\n' "$*"; }
die()  { printf '\033[0;31m[error]\033[0m %s\n' "$*" >&2; exit 1; }

usage() { grep -E '^#( |$)' "$0" | sed 's/^# \{0,1\}//'; exit 0; }

# ── args ─────────────────────────────────────────────────────────────────────────────────────────
while [ $# -gt 0 ]; do
  case "$1" in
    --native) MODE="native" ;;
    --edge-image) EDGE_IMAGE="$2"; shift ;;
    --bind) BIND="$2"; shift ;;
    --pspid) PSPID="$2"; shift ;;
    --edgeid) EDGEID="$2"; shift ;;
    --controlplane) CONTROLPLANE="$2"; shift ;;
    --data-dir) DATA_DIR="$2"; shift ;;
    --allow-subnet) ALLOW_SUBNET="$2"; shift ;;
    --enrollment-code) ENROLL_CODE="$2"; shift ;;
    --tls-client-auth) TLS_CLIENT_AUTH="$2"; shift ;;
    -h|--help) usage ;;
    *) die "unknown option: $1 (use --help)" ;;
  esac
  shift
done

[ "$(id -u)" -eq 0 ] || die "please run as root (sudo)."

# ── detect distro + package manager ──────────────────────────────────────────────────────────────
detect_platform() {
  DISTRO="unknown"; PKG=""
  if [ -r /etc/os-release ]; then . /etc/os-release; DISTRO="${ID:-unknown}"; fi
  for m in apt-get dnf yum zypper pacman apk; do
    if command -v "$m" >/dev/null 2>&1; then PKG="$m"; break; fi
  done
  log "Detected distro: ${DISTRO} · package manager: ${PKG:-none}"
}

pkg_install() {
  case "$PKG" in
    apt-get) DEBIAN_FRONTEND=noninteractive apt-get update -y && apt-get install -y "$@" ;;
    dnf)     dnf install -y "$@" ;;
    yum)     yum install -y "$@" ;;
    zypper)  zypper --non-interactive install -y "$@" ;;
    pacman)  pacman -Sy --noconfirm "$@" ;;
    apk)     apk add --no-cache "$@" ;;
    *) die "unsupported package manager; install these manually: $*" ;;
  esac
}

# ── container runtime ────────────────────────────────────────────────────────────────────────────
ensure_container_runtime() {
  if command -v docker >/dev/null 2>&1; then RUNTIME="docker"; COMPOSE="docker compose"; ok "docker present"; return; fi
  if command -v podman >/dev/null 2>&1; then RUNTIME="podman"; COMPOSE="podman compose"; ok "podman present"; return; fi
  log "No container runtime found — installing Docker via the official convenience script…"
  if curl -fsSL https://get.docker.com | sh; then
    systemctl enable --now docker || true
    RUNTIME="docker"; COMPOSE="docker compose"; ok "docker installed"
  else
    die "could not install Docker automatically. Install docker or podman, then re-run — or use --native."
  fi
}

# ── TLS material for the transaction API ─────────────────────────────────────────────────────────
# The edge terminates TLS itself and REFUSES to start without a keystore (no plaintext fallback).
# We generate a self-signed PKCS#12 so a fresh install is encrypted from the first request; replace
# it with a certificate from your own CA before going live (see the note printed at the end).
random_password() {
  local pw=""
  if command -v openssl >/dev/null 2>&1; then
    pw="$(openssl rand -base64 48 2>/dev/null | LC_ALL=C tr -dc 'A-Za-z0-9' | head -c 32)"
  fi
  if [ "${#pw}" -lt 32 ]; then
    pw="$(LC_ALL=C tr -dc 'A-Za-z0-9' < /dev/urandom | head -c 32)"
  fi
  printf '%s' "$pw"
}

ensure_tls_cert() {
  local store="$HERE/secrets/edge-tls.p12"
  local passfile="$HERE/secrets/edge-tls.pass"

  if [ -s "$store" ] && [ -s "$passfile" ]; then
    TLS_PASS="$(cat "$passfile")"
    ok "TLS keystore already present ($store) — leaving it untouched"
    return
  fi

  TLS_PASS="$(random_password)"
  local dname="CN=hokeka-edge,O=Hokeka Edge,OU=${PSPID}"
  local san="SAN=dns:localhost,ip:127.0.0.1"
  [ "$BIND" = "127.0.0.1" ] || san="${san},ip:${BIND}"

  log "Generating a self-signed TLS keystore for the transaction API…"
  if command -v keytool >/dev/null 2>&1; then
    keytool -genkeypair -alias edge -keyalg EC -groupname secp384r1 -sigalg SHA384withECDSA \
      -validity 825 -storetype PKCS12 -keystore "$store" -storepass "$TLS_PASS" -keypass "$TLS_PASS" \
      -dname "$dname" -ext "$san" >/dev/null 2>&1 || die "keytool failed to create $store"
  elif command -v openssl >/dev/null 2>&1; then
    local tmp; tmp="$(mktemp -d)"
    openssl req -x509 -newkey ec -pkeyopt ec_paramgen_curve:secp384r1 -sha384 -days 825 -nodes \
      -keyout "$tmp/key.pem" -out "$tmp/cert.pem" -subj "/CN=hokeka-edge/O=Hokeka Edge/OU=${PSPID}" \
      -addext "subjectAltName=DNS:localhost,IP:127.0.0.1" >/dev/null 2>&1 \
      || die "openssl failed to create the edge certificate"
    openssl pkcs12 -export -name edge -inkey "$tmp/key.pem" -in "$tmp/cert.pem" \
      -out "$store" -passout "pass:${TLS_PASS}" >/dev/null 2>&1 || die "openssl failed to build $store"
    rm -rf "$tmp"
  elif [ -n "${RUNTIME:-}" ]; then
    # No JDK/openssl on the host — borrow keytool from the edge image itself.
    $RUNTIME run --rm --entrypoint keytool -v "$HERE/secrets:/secrets" "$EDGE_IMAGE" \
      -genkeypair -alias edge -keyalg EC -groupname secp384r1 -sigalg SHA384withECDSA \
      -validity 825 -storetype PKCS12 -keystore /secrets/edge-tls.p12 \
      -storepass "$TLS_PASS" -keypass "$TLS_PASS" -dname "$dname" -ext "$san" >/dev/null 2>&1 \
      || die "could not generate the TLS keystore with keytool from ${EDGE_IMAGE}"
  else
    die "no keytool, openssl or container runtime available to generate the TLS keystore."
  fi

  printf '%s' "$TLS_PASS" > "$passfile"
  chmod 600 "$store" "$passfile"
  ok "TLS keystore created (self-signed, P-384, 825 days) — mode 600"
}

# ── firewall the local transaction API (optional) ────────────────────────────────────────────────
firewall_api() {
  [ -n "$ALLOW_SUBNET" ] || { warn "API bound to ${BIND}; pass --allow-subnet to add a host firewall rule"; return; }
  if command -v ufw >/dev/null 2>&1; then
    ufw allow from "$ALLOW_SUBNET" to any port 8443 proto tcp && ufw deny 8443/tcp && ok "ufw: 8443 limited to ${ALLOW_SUBNET}"
  elif command -v firewall-cmd >/dev/null 2>&1; then
    firewall-cmd --permanent --add-rich-rule="rule family=ipv4 source address=${ALLOW_SUBNET} port port=8443 protocol=tcp accept" \
      && firewall-cmd --reload && ok "firewalld: 8443 limited to ${ALLOW_SUBNET}"
  else
    warn "no ufw/firewalld found; restrict port 8443 to ${ALLOW_SUBNET} at your network layer"
  fi
}

# ── container install (default) ──────────────────────────────────────────────────────────────────
install_container() {
  ensure_container_runtime
  install -d -m 700 "$HERE/config" "$HERE/secrets" "$HERE/data"
  ok "created config/secrets/data (mode 700)"
  ensure_tls_cert

  cat > "$HERE/.env" <<EOF
EDGE_IMAGE=${EDGE_IMAGE}
EDGE_BIND=${BIND}
EDGE_PSPID=${PSPID}
EDGE_EDGEID=${EDGEID}
CONTROLPLANE_URL=${CONTROLPLANE}
AEROSPIKE_TAG=6.4
# TLS on the transaction API — the edge refuses to start without these.
EDGE_TLS_KEYSTORE_PASSWORD=${TLS_PASS}
EDGE_TLS_KEY_ALIAS=edge
EDGE_TLS_CLIENT_AUTH=${TLS_CLIENT_AUTH}
# First-boot activation + pinned control-plane keys (from portal.hokeka.com).
EDGE_ENROLLMENT_CODE=${ENROLL_CODE}
CONTROLPLANE_ED25519_PUBLIC_KEY=
CONTROLPLANE_X25519_PUBLIC_KEY=
# mTLS client identity issued to this edge (drop the PKCS#12 into secrets/ and set the password).
EDGE_MTLS_KEYSTORE=/opt/hokeka/secrets/edge-client.p12
EDGE_MTLS_KEYSTORE_PASSWORD=
# Optional: custom trust anchors for the control plane. Empty = the JDK default trust store.
EDGE_MTLS_TRUSTSTORE=
EDGE_MTLS_TRUSTSTORE_PASSWORD=
EOF
  chmod 600 "$HERE/.env"
  ok "wrote .env (mode 600)"

  log "Starting the isolated stack (Aerospike has NO published port — reachable only by the edge)…"
  ( cd "$HERE" && $COMPOSE up -d )
  firewall_api

  log "Waiting for health…"
  for i in $(seq 1 30); do
    if $RUNTIME exec hokeka-edge curl -fsk https://localhost:8443/actuator/health >/dev/null 2>&1; then
      ok "edge is HEALTHY"; break
    fi
    sleep 3
    [ "$i" = 30 ] && warn "edge not healthy yet — check: $COMPOSE logs edge"
  done

  print_summary "container"
}

# ── native install (--native) ────────────────────────────────────────────────────────────────────
install_native() {
  warn "Native mode: on Windows/macOS use install.ps1 / containers — this path is Linux-only."
  command -v curl >/dev/null 2>&1 || pkg_install curl
  command -v java >/dev/null 2>&1 || install_temurin25
  install_aerospike_native
  harden_aerospike_native
  install_edge_native
  print_summary "native"
}

install_temurin25() {
  log "Installing Temurin JRE 25…"
  case "$PKG" in
    apt-get)
      pkg_install wget apt-transport-https gnupg
      wget -qO- https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor -o /usr/share/keyrings/adoptium.gpg
      . /etc/os-release
      echo "deb [signed-by=/usr/share/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb ${VERSION_CODENAME} main" \
        > /etc/apt/sources.list.d/adoptium.list
      pkg_install temurin-25-jre ;;
    dnf|yum)
      cat > /etc/yum.repos.d/adoptium.repo <<'R'
[Adoptium]
name=Adoptium
baseurl=https://packages.adoptium.net/artifactory/rpm/rhel/$releasever/$basearch
enabled=1
gpgcheck=1
gpgkey=https://packages.adoptium.net/artifactory/api/gpg/key/public
R
      pkg_install temurin-25-jre ;;
    *) warn "install a JRE 25 (Temurin) for ${PKG} manually, then re-run" ;;
  esac
  ok "Java: $(java -version 2>&1 | head -1)"
}

install_aerospike_native() {
  log "Installing Aerospike Community Edition…"
  local tmp; tmp="$(mktemp -d)"
  case "$PKG" in
    apt-get) curl -fsSL "https://enterprise.aerospike.com/enterprise/download/server/latest/artifact/ubuntu22_amd64" -o "$tmp/aero.tgz" || \
             warn "adjust the Aerospike download URL for your distro (see aerospike.com/download)";;
    *) warn "download Aerospike CE for ${DISTRO} from aerospike.com/download and install, then re-run";;
  esac
  install -d -m 750 -o root "$DATA_DIR/aerospike/data" 2>/dev/null || true
  install -m 640 "$HERE/aerospike/aerospike.conf" /etc/aerospike/aerospike.conf 2>/dev/null || \
    { install -d /etc/aerospike; install -m 640 "$HERE/aerospike/aerospike.conf" /etc/aerospike/aerospike.conf; }
  ok "Aerospike config staged"
}

harden_aerospike_native() {
  log "Hardening the database to app-only access…"
  # 1) Bind Aerospike to loopback so nothing off-box can connect.
  sed -i 's/address any\b.*/address 127.0.0.1   # hardened: loopback only/' /etc/aerospike/aerospike.conf || true
  # 2) Firewall port 3000 to localhost only, regardless of bind.
  if command -v ufw >/dev/null 2>&1; then
    ufw deny 3000/tcp && ok "ufw: port 3000 denied from the network (localhost still works)"
  elif command -v firewall-cmd >/dev/null 2>&1; then
    firewall-cmd --permanent --remove-port=3000/tcp 2>/dev/null || true; firewall-cmd --reload || true
    ok "firewalld: port 3000 not exposed"
  elif command -v iptables >/dev/null 2>&1; then
    iptables -A INPUT -p tcp --dport 3000 ! -s 127.0.0.1 -j DROP && ok "iptables: 3000 dropped except localhost"
  else
    warn "no firewall tool found — ensure port 3000 is blocked from the network"
  fi
  systemctl enable --now aerospike 2>/dev/null || warn "start Aerospike once installed: systemctl start aerospike"
  warn "Aerospike CE has no DB auth. For DB-level user/password + TLS, use Aerospike ENTERPRISE and enable the security{} stanza."
}

install_edge_native() {
  log "Installing the edge host (tarball)…"
  install -d -m 700 "$DATA_DIR"/{config,secrets,bin,data}
  install -d -m 700 "$HERE/secrets"
  ensure_tls_cert
  install -m 600 "$HERE/secrets/edge-tls.p12" "$DATA_DIR/secrets/edge-tls.p12"
  cat > "$DATA_DIR/config/edge.env" <<EOF
EDGE_BIND_ADDRESS=${BIND}
EDGE_TLS_KEYSTORE=${DATA_DIR}/secrets/edge-tls.p12
EDGE_TLS_KEYSTORE_PASSWORD=${TLS_PASS}
EDGE_TLS_KEY_ALIAS=edge
EDGE_TLS_CLIENT_AUTH=${TLS_CLIENT_AUTH}
EDGE_PSPID=${PSPID}
EDGE_EDGEID=${EDGEID}
CONTROLPLANE_URL=${CONTROLPLANE}
EDGE_ENROLLMENT_CODE=${ENROLL_CODE}
CONTROLPLANE_ED25519_PUBLIC_KEY=
CONTROLPLANE_X25519_PUBLIC_KEY=
EDGE_MTLS_KEYSTORE=${DATA_DIR}/secrets/edge-client.p12
EDGE_MTLS_KEYSTORE_PASSWORD=
EDGE_IDENTITY_FILE=${DATA_DIR}/data/edge-identity.json
EOF
  chmod 600 "$DATA_DIR/config/edge.env"
  ok "TLS keystore + ${DATA_DIR}/config/edge.env staged (mode 600)"
  warn "Fetch the edge tarball from packages.hokeka.com and extract to ${DATA_DIR} (see the install guide §7),"
  warn "then enable the service: systemctl enable --now hokeka-edge"
}

print_summary() {
  echo
  log "──────────── Installation summary (${1}) ────────────"
  ok  "Edge transaction API : https://${BIND}:8443  (PSP API nodes only)"
  ok  "Database access      : Aerospike is reachable ONLY by the edge app"
  if [ "$1" = "container" ]; then
    echo "                         (private Docker network, no published port)"
  else
    echo "                         (bound to 127.0.0.1 + firewalled on port 3000)"
  fi
  ok  "Control plane        : ${CONTROLPLANE}  (outbound only, mTLS + HSE-1)"
  ok  "Transport security   : TLS 1.3 only, self-signed keystore at secrets/edge-tls.p12"
  echo
  warn "The generated certificate is SELF-SIGNED. Before production, replace it with one from your"
  echo  "  own CA (same alias 'edge'), keeping the same path and updating EDGE_TLS_KEYSTORE_PASSWORD:"
  echo  "    keytool -importkeystore -srckeystore your-ca-issued.p12 -srcstoretype PKCS12 \\"
  echo  "            -destkeystore secrets/edge-tls.p12 -deststoretype PKCS12 -alias edge"
  echo  "  The edge refuses to start if the keystore is missing — it never falls back to plain HTTP."
  echo
  if [ -z "$ENROLL_CODE" ]; then
    warn "No --enrollment-code given: this node stays UNAUTHORIZED and will only return HOLD."
  fi
  log "Next: request an enrollment code at portal.hokeka.com, set EDGE_ENROLLMENT_CODE plus the pinned"
  log "      CONTROLPLANE_*_PUBLIC_KEY values in .env, then approve the node in the portal."
  if [ "$1" = "container" ]; then
    log "Manage: cd $HERE && $COMPOSE ps | logs | down"
  fi
}

# ── run ──────────────────────────────────────────────────────────────────────────────────────────
detect_platform
if [ "$MODE" = "native" ]; then install_native; else install_container; fi
