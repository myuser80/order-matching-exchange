#!/bin/bash
# =============================================================================
# test-orders.sh  –  End-to-end smoke test for the Order Matching Platform
# =============================================================================
# Usage:
#   chmod +x scripts/test-orders.sh
#   ./scripts/test-orders.sh
# =============================================================================

GATEWAY="http://localhost:8081/api/v1/orders"
EXEC="http://localhost:8083/api/v1/executions"
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
RED='\033[0;31m'
NC='\033[0m'

header() { echo -e "\n${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"; echo -e "${CYAN}  $1${NC}"; echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"; }
ok()     { echo -e "${GREEN}  ✔  $1${NC}"; }
info()   { echo -e "${YELLOW}  ➜  $1${NC}"; }
err()    { echo -e "${RED}  ✖  $1${NC}"; }

wait_for_service() {
  local url=$1
  local name=$2
  info "Waiting for $name..."
  for i in $(seq 1 30); do
    if curl -sf "$url" > /dev/null 2>&1; then
      ok "$name is UP"
      return 0
    fi
    sleep 2
  done
  err "$name did not start in time"
  exit 1
}

# ─── Wait for services ────────────────────────────────────────────────────────
header "Checking Services"
wait_for_service "$GATEWAY/health"              "Order Gateway (8081)"
wait_for_service "http://localhost:8082/actuator/health" "Matching Engine (8082)"
wait_for_service "$EXEC/health"                 "Execution Service (8083)"

# ─── Scenario 1: Full Match ────────────────────────────────────────────────────
header "Scenario 1: Full Match — SELL 100 @ ₹500, BUY 100 @ ₹500"

info "Submitting SELL order (SELLER1 - TCS, 100 shares @ ₹500)"
SELL1=$(curl -sf -X POST "$GATEWAY" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "SELLER1",
    "ticker":   "TCS",
    "side":     "SELL",
    "limitPrice": 500.00,
    "quantity": 100,
    "timeInForce": "GTC"
  }')
echo "  Response: $SELL1"
SELL1_ID=$(echo $SELL1 | grep -o '"orderId":"[^"]*"' | cut -d'"' -f4)
ok "Sell order created: $SELL1_ID"

sleep 1

info "Submitting BUY order (BUYER1 - TCS, 100 shares @ ₹500)"
BUY1=$(curl -sf -X POST "$GATEWAY" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "BUYER1",
    "ticker":   "TCS",
    "side":     "BUY",
    "limitPrice": 500.00,
    "quantity": 100,
    "timeInForce": "GTC"
  }')
echo "  Response: $BUY1"
BUY1_ID=$(echo $BUY1 | grep -o '"orderId":"[^"]*"' | cut -d'"' -f4)
ok "Buy order created: $BUY1_ID"

sleep 2
info "Checking trades for TCS..."
TRADES=$(curl -sf "$EXEC/trades?ticker=TCS")
echo "  Trades: $TRADES"

# ─── Scenario 2: Partial Fill ─────────────────────────────────────────────────
header "Scenario 2: Partial Fill — SELL 50 @ ₹1500, BUY 100 @ ₹1500"

curl -sf -X POST "$GATEWAY" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "SELLER2",
    "ticker":   "RELIANCE",
    "side":     "SELL",
    "limitPrice": 1500.00,
    "quantity": 50,
    "timeInForce": "GTC"
  }' > /dev/null
ok "Sell 50 RELIANCE @ 1500 submitted"

sleep 1

curl -sf -X POST "$GATEWAY" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "BUYER2",
    "ticker":   "RELIANCE",
    "side":     "BUY",
    "limitPrice": 1500.00,
    "quantity": 100,
    "timeInForce": "GTC"
  }' > /dev/null
ok "Buy 100 RELIANCE @ 1500 submitted (should partially fill 50)"

sleep 2
info "Checking trades for RELIANCE..."
curl -sf "$EXEC/trades?ticker=RELIANCE"
echo ""

# ─── Scenario 3: Price Improvement ────────────────────────────────────────────
header "Scenario 3: Price Improvement — Buyer pays less than willing"

curl -sf -X POST "$GATEWAY" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "SELLER3",
    "ticker":   "INFY",
    "side":     "SELL",
    "limitPrice": 1400.00,
    "quantity": 20,
    "timeInForce": "GTC"
  }' > /dev/null
ok "Sell 20 INFY @ ₹1400 submitted"

sleep 1

curl -sf -X POST "$GATEWAY" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "BUYER3",
    "ticker":   "INFY",
    "side":     "BUY",
    "limitPrice": 1500.00,
    "quantity": 20,
    "timeInForce": "GTC"
  }' > /dev/null
ok "Buy 20 INFY @ ₹1500 (willing to pay ₹1500, executes at ₹1400!)"

sleep 2
info "Checking trades for INFY..."
curl -sf "$EXEC/trades?ticker=INFY"
echo ""

# ─── Scenario 4: IOC ──────────────────────────────────────────────────────────
header "Scenario 4: IOC — Immediate Or Cancel"

curl -sf -X POST "$GATEWAY" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "SELLER4",
    "ticker":   "HDFC",
    "side":     "SELL",
    "limitPrice": 1600.00,
    "quantity": 10,
    "timeInForce": "GTC"
  }' > /dev/null
ok "Sell 10 HDFC @ 1600"

sleep 1

IOC_ORDER=$(curl -sf -X POST "$GATEWAY" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "BUYER4",
    "ticker":   "HDFC",
    "side":     "BUY",
    "limitPrice": 1600.00,
    "quantity": 30,
    "timeInForce": "IOC"
  }')
ok "IOC Buy 30 HDFC @ 1600 submitted — only 10 available → fills 10, cancels remaining 20"
echo "  Order: $IOC_ORDER"

sleep 2

# ─── Scenario 5: No Match (Price Gap) ────────────────────────────────────────
header "Scenario 5: No Match — Price Gap"

curl -sf -X POST "$GATEWAY" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "SELLER5",
    "ticker":   "WIPRO",
    "side":     "SELL",
    "limitPrice": 300.00,
    "quantity": 50,
    "timeInForce": "GTC"
  }' > /dev/null
ok "Sell 50 WIPRO @ ₹300"

curl -sf -X POST "$GATEWAY" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "BUYER5",
    "ticker":   "WIPRO",
    "side":     "BUY",
    "limitPrice": 290.00,
    "quantity": 50,
    "timeInForce": "GTC"
  }' > /dev/null
ok "Buy 50 WIPRO @ ₹290 — prices don't cross → no trade, both rest in book"

sleep 2
info "Trades for WIPRO (should be empty):"
curl -sf "$EXEC/trades?ticker=WIPRO"
echo ""

# ─── Summary ──────────────────────────────────────────────────────────────────
header "All Trades Summary"
info "All executed trades:"
curl -sf "$EXEC/all" | python3 -m json.tool 2>/dev/null || curl -sf "$EXEC/all"
echo ""

info "Trades by client BUYER1:"
curl -sf "$EXEC/client?clientId=BUYER1"
echo ""

ok "Test script complete! Check matching-engine logs for execution details."
echo ""
echo -e "${CYAN}Useful endpoints:${NC}"
echo "  Order Gateway:    http://localhost:8081/api/v1/orders"
echo "  Matching Engine:  http://localhost:8082/actuator/health"
echo "  Execution Svc:    http://localhost:8083/api/v1/executions/all"
echo "  Kafka UI:         http://localhost:9090"
echo "  H2 Console (GW):  http://localhost:8081/h2-console"
echo ""
