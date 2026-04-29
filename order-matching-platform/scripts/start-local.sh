#!/bin/bash
# =============================================================================
# start-local.sh — Start all services locally (Kafka via Docker, apps via Maven)
# =============================================================================

set -e
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
ok()   { echo -e "${GREEN}✔ $1${NC}"; }
info() { echo -e "${YELLOW}➜ $1${NC}"; }

# 1. Start only infrastructure
info "Starting Kafka + Zookeeper..."
docker-compose up -d zookeeper kafka kafka-ui
sleep 10
ok "Infrastructure up"

# 2. Build everything
info "Building all modules..."
mvn clean package -DskipTests -q
ok "Build complete"

# 3. Start services in background
info "Starting Order Gateway on :8081..."
java -jar order-gateway/target/order-gateway-1.0.0.jar &
GW_PID=$!

info "Starting Matching Engine on :8082..."
java -jar matching-engine/target/matching-engine-1.0.0.jar &
ME_PID=$!

info "Starting Execution Service on :8083..."
java -jar execution-service/target/execution-service-1.0.0.jar &
ES_PID=$!

ok "All services started. PIDs: GW=$GW_PID ME=$ME_PID ES=$ES_PID"
echo ""
echo "Press CTRL+C to stop all services"

trap "kill $GW_PID $ME_PID $ES_PID 2>/dev/null; docker-compose stop kafka zookeeper; echo 'Stopped.'" EXIT INT TERM
wait
