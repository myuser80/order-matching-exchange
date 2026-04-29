# 🏦 Order Matching Platform
### A Simplified Stock Exchange (like BSE / NSE) built with Java 17 + Spring Boot 3 + Apache Kafka

---

## 🎓 What Is This? (For Beginners)

Imagine you're at a **school canteen auction** 🏫:

| Person | What they want |
|--------|---------------|
| **Riya** (Buyer) | Wants to buy 10 sandwiches, willing to pay **₹50 each** |
| **Arjun** (Seller) | Has 10 sandwiches, wants at least **₹50 each** |
| **Canteen Manager** (Matching Engine) | Sees both → says *"Deal! ₹50 × 10 = ₹500"* |
| **Receipt Machine** (Execution Service) | Gives both a receipt confirming the trade |

That's exactly what **BSE / NSE** does — but with **millions of buy/sell orders per second** for stocks!

### Key Terms (Plain English)

| Term | What It Means |
|------|--------------|
| **Order** | "I want to BUY / SELL X shares of RELIANCE at ₹Y" |
| **Order Book** | A sorted list of all waiting buy/sell orders for one stock |
| **Matching Engine** | The brain that finds buy/sell pairs and executes trades |
| **Trade** | When a buyer and seller agree — money and shares exchange |
| **Execution Report** | The confirmation sent to both buyer and seller |
| **Price-Time Priority** | Best price matched first; if same price, earlier order wins |
| **GTC** | Good Till Cancelled — order stays until fully filled or manually cancelled |
| **IOC** | Immediate Or Cancel — fill what you can right now, cancel the rest |
| **FOK** | Fill Or Kill — fill 100% right now or cancel the entire order |

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      CLIENT / TRADER                            │
│          (REST API via curl, Postman, or your frontend)         │
└────────────────────────┬────────────────────────────────────────┘
                         │ POST /api/v1/orders
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                   ORDER GATEWAY  :8081                          │
│  • Validates incoming orders (side, price, qty, TIF)           │
│  • Persists order to H2/PostgreSQL (status = NEW)              │
│  • Publishes to Kafka topic: order.submitted                    │
│  • Key = ticker (guarantees ordering per instrument)           │
└────────────────────────┬────────────────────────────────────────┘
                         │ Kafka: order.submitted
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                MATCHING ENGINE  :8082                           │
│  • Consumes orders from Kafka (partitioned by ticker)          │
│  • Maintains in-memory Order Book per ticker                   │
│  • Price-Time Priority matching algorithm                      │
│  • Handles GTC / IOC / FOK semantics                           │
│  • Publishes trades to Kafka topic: trade.executed             │
└────────────────────────┬────────────────────────────────────────┘
                         │ Kafka: trade.executed
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│               EXECUTION SERVICE  :8083                          │
│  • Consumes trade events from Kafka                            │
│  • Persists trade records to H2/PostgreSQL                     │
│  • Generates execution reports for both counterparties         │
│  • Emits metrics (Prometheus / Micrometer)                     │
│  • REST API to query trade history                             │
└─────────────────────────────────────────────────────────────────┘
```

### How a Trade Flows (Step by Step)

```
1. Trader submits: BUY 100 RELIANCE @ ₹2800
2. Order Gateway validates → saves order → publishes to Kafka
3. Matching Engine receives order → checks sell book for RELIANCE
4. Finds: SELL 100 RELIANCE @ ₹2800 (resting order)
5. MATCH! Execute at ₹2800 × 100 shares
6. Trade event published to Kafka
7. Execution Service persists trade
8. Both buyer & seller get execution reports
```

### Order Book Visualized

```
RELIANCE Order Book
════════════════════════════════════════
BID (BUYERS)              ASK (SELLERS)
Price    Qty   Client    Price    Qty   Client
2810     200   BUYER_A   2820     150   SELLER_X
2800     500   BUYER_B   2825     300   SELLER_Y
2795     100   BUYER_C   2830     200   SELLER_Z
════════════════════════════════════════
Best Bid: ₹2810   Best Ask: ₹2820
→ No match yet (2810 < 2820)

If BUYER_A raises bid to ₹2820:
→ TRADE at ₹2820! (seller's resting price)
```

---

## 📁 Project Structure

```
order-matching-platform/
├── pom.xml                          # Parent Maven POM
├── docker-compose.yml               # Full platform (Kafka + 3 services)
├── README.md
│
├── common-lib/                      # Shared models, events, enums
│   └── src/main/java/com/exchange/
│       ├── model/
│       │   ├── Order.java           # Core Order domain object
│       │   └── Trade.java           # Core Trade domain object
│       ├── events/
│       │   ├── KafkaTopics.java     # Topic name constants
│       │   └── ExecutionReport.java
│       └── enums/
│           ├── OrderSide.java       # BUY / SELL
│           ├── OrderStatus.java     # NEW / PARTIALLY_FILLED / FILLED / CANCELLED
│           └── TimeInForce.java     # GTC / IOC / FOK
│
├── order-gateway/                   # Service 1: REST API entry point
│   └── src/main/java/com/exchange/
│       ├── controller/OrderController.java
│       ├── service/OrderService.java
│       ├── kafka/OrderProducer.java
│       ├── entity/OrderEntity.java
│       ├── repository/OrderRepository.java
│       └── config/KafkaProducerConfig.java
│
├── matching-engine/                 # Service 2: Core matching logic
│   └── src/main/java/com/exchange/
│       ├── engine/
│       │   ├── OrderBook.java       # ★ Price-Time Priority Order Book
│       │   └── MatchingAlgorithm.java  # ★ Core matching algorithm
│       ├── service/MatchingService.java
│       ├── kafka/OrderConsumer.java
│       └── config/
│
├── execution-service/               # Service 3: Trade persistence & reporting
│   └── src/main/java/com/exchange/
│       ├── controller/ExecutionController.java
│       ├── service/ExecutionService.java
│       ├── kafka/TradeConsumer.java
│       ├── entity/TradeEntity.java
│       └── repository/TradeRepository.java
│
└── scripts/
    ├── test-orders.sh               # End-to-end test scenarios
    └── start-local.sh              # Local dev startup
```

---

## 🚀 Quick Start

### Prerequisites

| Tool | Version | Check |
|------|---------|-------|
| Java | 17+ | `java -version` |
| Maven | 3.8+ | `mvn -version` |
| Docker | 24+ | `docker --version` |
| Docker Compose | 2+ | `docker compose version` |
| curl | any | `curl --version` |

---

### Option A: Full Docker Setup (Recommended ✅)

Everything runs in Docker — no local Java needed after building.

```bash
# 1. Clone / extract the project
cd order-matching-platform

# 2. Start everything (Kafka + all 3 services)
docker-compose up --build

# 3. Wait ~60 seconds for all services to be healthy
# Watch logs:
docker-compose logs -f

# 4. In a new terminal, run the test scenarios
chmod +x scripts/test-orders.sh
./scripts/test-orders.sh
```

---

### Option B: Local Development (Kafka in Docker, Apps local)

```bash
# 1. Start only infrastructure
docker-compose up -d zookeeper kafka kafka-ui

# 2. Build all modules
mvn clean package -DskipTests

# 3. Start each service in separate terminals

# Terminal 1 – Order Gateway
java -jar order-gateway/target/order-gateway-1.0.0.jar

# Terminal 2 – Matching Engine
java -jar matching-engine/target/matching-engine-1.0.0.jar

# Terminal 3 – Execution Service
java -jar execution-service/target/execution-service-1.0.0.jar

# Or use the convenience script:
chmod +x scripts/start-local.sh
./scripts/start-local.sh
```

---

### Option C: Run Tests Only (No Kafka needed)

```bash
# Unit tests (MatchingAlgorithm) — pure Java, no infrastructure needed
mvn test -pl matching-engine

# All tests
mvn test
```

---

## 🧪 Manual Testing with curl

### Submit a SELL Order
```bash
curl -X POST http://localhost:8081/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "SELLER1",
    "ticker": "RELIANCE",
    "side": "SELL",
    "limitPrice": 2800.00,
    "quantity": 100,
    "timeInForce": "GTC"
  }'
```

### Submit a BUY Order (will match the SELL above!)
```bash
curl -X POST http://localhost:8081/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "BUYER1",
    "ticker": "RELIANCE",
    "side": "BUY",
    "limitPrice": 2800.00,
    "quantity": 100,
    "timeInForce": "GTC"
  }'
```

### Check Trades
```bash
# All trades for RELIANCE
curl http://localhost:8083/api/v1/executions/trades?ticker=RELIANCE

# All trades by a client
curl http://localhost:8083/api/v1/executions/client?clientId=BUYER1

# All trades
curl http://localhost:8083/api/v1/executions/all
```

### Check Orders
```bash
# Orders by client
curl http://localhost:8081/api/v1/orders?clientId=SELLER1

# Specific order
curl http://localhost:8081/api/v1/orders/{orderId}
```

---

## 📊 Scenarios Explained

### 1. Full Match
```
SELL 100 TCS @ ₹3500  (resting in book)
BUY  100 TCS @ ₹3500  (incoming)
→ TRADE: 100 TCS @ ₹3500 ✅
```

### 2. Partial Fill
```
SELL 50 RELIANCE @ ₹2800  (resting)
BUY 100 RELIANCE @ ₹2800  (incoming)
→ TRADE: 50 @ ₹2800
→ Buyer still needs 50 more (rests in book if GTC)
```

### 3. Price Improvement (Buyer Saves Money!)
```
SELL 20 INFY @ ₹1400  (resting — seller wants at least 1400)
BUY  20 INFY @ ₹1500  (incoming — buyer willing to pay up to 1500)
→ TRADE: 20 @ ₹1400  ← Buyer pays LESS than willing! (price-time priority)
```

### 4. IOC — Immediate Or Cancel
```
SELL 10 HDFC @ ₹1600  (only 10 available)
BUY  30 HDFC @ ₹1600  IOC  (want 30 immediately)
→ Fills 10 immediately
→ Remaining 20 CANCELLED (IOC = no waiting)
```

### 5. FOK — Fill Or Kill
```
SELL 10 WIPRO @ ₹300  (only 10 available)
BUY  30 WIPRO @ ₹300  FOK  (need all 30 or nothing)
→ Cannot fill all 30 → ENTIRE ORDER CANCELLED
```

---

## 🌐 Service Endpoints

| Service | Port | Key Endpoints |
|---------|------|--------------|
| **Order Gateway** | 8081 | `POST /api/v1/orders` — Submit order |
| | | `GET /api/v1/orders?clientId=X` — Get orders |
| | | `GET /h2-console` — Database UI |
| **Matching Engine** | 8082 | `GET /actuator/health` |
| | | `GET /actuator/prometheus` — Metrics |
| **Execution Service** | 8083 | `GET /api/v1/executions/all` — All trades |
| | | `GET /api/v1/executions/trades?ticker=X` |
| | | `GET /api/v1/executions/client?clientId=X` |
| **Kafka UI** | 9090 | Browse topics, messages, consumer groups |

---

## ⚙️ Design Decisions & Trade-offs

### Why Kafka?
- **Decoupling**: Services don't call each other directly → independent scaling
- **Durability**: Orders not lost if Matching Engine restarts
- **Ordering**: Ticker-keyed messages → same ticker always goes to same partition → sequential processing per instrument
- **Replay**: Can reprocess orders from any point in time

### Why Price-Time Priority?
- **Fairness**: Best price always gets executed first
- **Deterministic**: No ambiguity — same input always produces same output
- **Industry standard**: Used by BSE, NSE, NYSE, NASDAQ

### In-Memory Order Book
- **Ultra-low latency**: No DB round-trip during matching (microseconds)
- **Trade-off**: State lost on restart → production systems use event sourcing to rebuild from Kafka

### Horizontal Scaling
- **Kafka partitions by ticker**: Add more matching-engine instances → each handles a subset of tickers
- **Stateless gateway**: Multiple gateways behind a load balancer
- **Stateless execution service**: Multiple instances consuming from different partitions

### Performance Considerations
| Concern | Approach |
|---------|---------|
| **Throughput** | Kafka batching (linger.ms=1, batch.size=16KB), LZ4 compression |
| **Latency** | In-memory order book (TreeMap), no DB per match |
| **Backpressure** | Kafka consumer max.poll.records=100, manual ack |
| **Fault Tolerance** | Kafka consumer groups, at-least-once delivery |
| **Observability** | Micrometer + Prometheus metrics on every service |

---

## 📈 Kafka Topics

| Topic | Producer | Consumer | Key |
|-------|----------|----------|-----|
| `order.submitted` | Order Gateway | Matching Engine | ticker |
| `trade.executed` | Matching Engine | Execution Service | ticker |
| `execution.report` | Execution Service | Traders (future) | clientId |

---

## 🔬 Running Unit Tests

```bash
# All tests
mvn test

# Only matching engine tests (no Kafka/Spring needed)
mvn test -pl matching-engine

# Verbose output
mvn test -pl matching-engine -Dsurefire.useFile=false
```

### Test Coverage

| Test | What It Validates |
|------|------------------|
| Full match at same price | Basic buy/sell matching |
| Buy price > sell price | Price improvement, execution at seller's price |
| No match (price gap) | Orders rest in book |
| Partial fill | Large order vs small counter-order |
| Multiple fills | One order matched against several counter-orders |
| IOC cancel remainder | Time-in-force IOC semantics |
| FOK full cancel | Time-in-force FOK with insufficient liquidity |
| FOK success | Time-in-force FOK with sufficient liquidity |
| Price-time priority | Best-priced order matched first |

---

## 🐛 Troubleshooting

### Services won't start
```bash
# Check Kafka is healthy
docker-compose ps
docker-compose logs kafka

# Force recreate
docker-compose down -v
docker-compose up --build
```

### No trades being created
```bash
# Check matching engine logs
docker-compose logs -f matching-engine

# Check Kafka topics have messages
# Open Kafka UI: http://localhost:9090
```

### Port already in use
```bash
# Kill process on port
lsof -ti:8081 | xargs kill
lsof -ti:8082 | xargs kill
lsof -ti:8083 | xargs kill
lsof -ti:9092 | xargs kill
```

### H2 Console (view order/trade data)
- URL: `http://localhost:8081/h2-console`
- JDBC URL: `jdbc:h2:mem:orderdb`
- Username: `sa` | Password: `password`

---

## 🔮 Production Enhancements (What's Next)

| Feature | How |
|---------|-----|
| **Persistent order book** | PostgreSQL + event sourcing (Axon Framework) |
| **WebSocket notifications** | Spring WebSocket → push execution reports to traders |
| **FIX Protocol** | QuickFIX/J for institutional connectivity |
| **Risk checks** | Pre-trade validation service (credit limits, position limits) |
| **Market orders** | Execute at any available price |
| **Stop orders** | Trigger when price crosses threshold |
| **Redis caching** | Cache order book snapshots |
| **Distributed order book** | Multiple matching engine instances with Hazelcast |
| **Grafana dashboard** | Prometheus metrics → visual dashboards |
| **Circuit breakers** | Resilience4j on Kafka producers |

---

## 📚 Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| Message Bus | Apache Kafka 7.5 |
| Persistence | H2 (dev) / PostgreSQL (prod) |
| Build | Maven 3.9 |
| Containers | Docker + Docker Compose |
| Observability | Micrometer + Prometheus |
| Testing | JUnit 5 + AssertJ |
| Serialization | Jackson JSON |

---

*Built with ❤️ as a learning reference for distributed systems and financial exchange architecture.*
