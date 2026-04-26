# NixSpace API

A production-grade, real-time messaging platform API built with **Java 21**, **Spring Boot 3.3**, **MySQL 8**, **Redis 7**, and **Kafka** — following the architecture in the system design document.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Tech Stack](#tech-stack)
3. [Project Structure](#project-structure)
4. [Prerequisites](#prerequisites)
5. [Quick Start](#quick-start)
6. [Configuration](#configuration)
7. [API Reference](#api-reference)
8. [WebSocket / Real-Time](#websocket--real-time)
9. [Authentication Flow](#authentication-flow)
10. [File Upload Flow](#file-upload-flow)
11. [Running Tests](#running-tests)
12. [Design Decisions](#design-decisions)
13. [Production Checklist](#production-checklist)

---

## Architecture Overview

```
Clients (Web / Mobile / Desktop)
        │
        ├── REST  ──► Spring Boot API (port 8080)
        │                    │
        │              ┌─────┼──────────────────┐
        │              │     │                  │
        │           MySQL  Redis             Kafka
        │           (data) (cache/presence)  (events)
        │                                       │
        └── WebSocket ──► STOMP Gateway ◄────────┘
                          (fan-out to subscribers)
```

**Request flow for a new message:**
1. Client → `POST /api/v1/channels/{id}/messages`
2. `MessageService` validates membership and persists to **MySQL**
3. Event published to **Kafka** (`nixspace.message.events`)
4. `KafkaMessageConsumer` consumes event → pushes via **STOMP** to `/topic/channels/{id}`
5. All subscribed WebSocket clients receive the message in real-time

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 (Virtual Threads ready) |
| Framework | Spring Boot 3.3 |
| Security | Spring Security + JWT (jjwt 0.12) |
| Database | MySQL 8.x + Spring Data JPA / Hibernate |
| Migrations | Flyway |
| Cache | Redis 7 (Spring Cache + Lettuce) |
| Messaging | Apache Kafka (Spring Kafka) |
| WebSocket | STOMP over SockJS |
| File Storage | AWS S3 (presigned URL pattern) |
| Mapping | MapStruct |
| Boilerplate | Lombok |
| API Docs | SpringDoc OpenAPI 3 / Swagger UI |
| Testing | JUnit 5, Mockito, MockMvc, Testcontainers |
| Build | Maven 3.9 |
| Container | Docker + Docker Compose |

---

## Project Structure

```
nixspace/
├── src/main/java/com/nixspace/
│   ├── NixSpaceApplication.java          # Entry point
│   ├── config/                           # Spring config beans
│   │   ├── AsyncConfig.java
│   │   ├── AwsConfig.java
│   │   ├── KafkaConfig.java
│   │   ├── NixSpaceProperties.java       # Type-safe @ConfigurationProperties
│   │   ├── OpenApiConfig.java
│   │   ├── RedisConfig.java
│   │   ├── SecurityConfig.java
│   │   └── WebSocketConfig.java
│   ├── security/                         # JWT + UserDetails
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── JwtTokenProvider.java
│   │   ├── UserDetailsServiceImpl.java
│   │   └── UserPrincipal.java
│   ├── domain/
│   │   ├── model/                        # JPA entities
│   │   ├── repository/                   # Spring Data JPA repos
│   │   ├── service/                      # Service interfaces
│   │   └── service/impl/                 # Service implementations
│   ├── api/
│   │   ├── controller/                   # REST controllers
│   │   ├── dto/request/                  # Validated input records
│   │   ├── dto/response/                 # Output records
│   │   ├── mapper/                       # MapStruct mappers
│   │   └── exception/                    # Global error handling
│   ├── infrastructure/
│   │   ├── kafka/                        # Producer + Consumer
│   │   ├── redis/                        # Presence service
│   │   └── s3/                           # Presigned URL service
│   └── common/
│       ├── enums/                        # Domain enumerations
│       └── util/                         # Scheduled tasks
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/
│       └── V1__initial_schema.sql        # Flyway baseline
├── src/test/java/com/nixspace/
│   ├── api/controller/                   # MockMvc controller tests
│   ├── domain/service/impl/              # Service unit tests
│   └── security/                         # JWT tests
├── docker-compose.yml                    # Local dev stack
├── Dockerfile                            # Multi-stage production build
└── pom.xml
```

---

## Prerequisites

| Tool | Version |
|---|---|
| Java | 21+ |
| Maven | 3.9+ |
| Docker & Docker Compose | Latest |
| AWS credentials | For S3 (or use LocalStack) |

---

## Quick Start

### 1. Clone and configure

```bash
git clone https://github.com/your-org/nixspace-api.git
cd nixspace-api

# Copy and edit environment variables
cp .env.example .env
```

### 2. Start infrastructure

```bash
# Starts MySQL, Redis, Kafka, Kafka UI
docker-compose up -d mysql redis zookeeper kafka

# Verify health
docker-compose ps
```

### 3. Run the API

```bash
mvn spring-boot:run
```

Or run everything including the API container:

```bash
docker-compose up --build
```

### 4. Explore the API

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api-docs
- **Health**: http://localhost:8080/actuator/health
- **Kafka UI**: http://localhost:8090

---

## Configuration

All settings live in `src/main/resources/application.yml` and are overridable via environment variables:

| Env Var | Description | Default |
|---|---|---|
| `DB_HOST` | MySQL host | `localhost` |
| `DB_PORT` | MySQL port | `3306` |
| `DB_NAME` | Database name | `nixspace` |
| `DB_USER` | DB username | `nixspace` |
| `DB_PASSWORD` | DB password | `nixspace` |
| `REDIS_HOST` | Redis host | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `KAFKA_HOSTS` | Kafka bootstrap servers | `localhost:9092` |
| `JWT_SECRET` | JWT signing secret (min 32 chars) | ⚠️ change in prod |
| `AWS_REGION` | AWS region | `us-east-1` |
| `S3_BUCKET` | S3 bucket name | `nixspace-files` |
| `SERVER_PORT` | API port | `8080` |

---

## API Reference

Base URL: `http://localhost:8080/api/v1`

All endpoints except `/auth/**` require `Authorization: Bearer <access_token>`.

### Authentication

| Method | Path | Description |
|---|---|---|
| `POST` | `/auth/register` | Create account → returns token pair |
| `POST` | `/auth/login` | Login → returns token pair |
| `POST` | `/auth/refresh` | Rotate refresh token |
| `POST` | `/auth/logout` | Revoke current device token |
| `POST` | `/auth/logout-all` | Revoke all device tokens |
| `PUT`  | `/auth/password` | Change password |

### Users

| Method | Path | Description |
|---|---|---|
| `GET`   | `/users/me` | Get own profile |
| `PATCH` | `/users/me` | Update display name / status / timezone |
| `GET`   | `/users/{userId}` | Get user summary by ID |
| `GET`   | `/users/search?workspaceId=&query=` | Search users in workspace |

### Workspaces

| Method | Path | Description |
|---|---|---|
| `POST`   | `/workspaces` | Create workspace |
| `GET`    | `/workspaces` | List my workspaces |
| `GET`    | `/workspaces/{id}` | Get workspace |
| `GET`    | `/workspaces/slug/{slug}` | Get workspace by slug |
| `PATCH`  | `/workspaces/{id}` | Update name/description |
| `DELETE` | `/workspaces/{id}` | Delete workspace (owner only) |
| `GET`    | `/workspaces/{id}/members` | List members |
| `POST`   | `/workspaces/{id}/members` | Invite member by email |
| `PATCH`  | `/workspaces/{id}/members/{userId}/role` | Update member role |
| `DELETE` | `/workspaces/{id}/members/{userId}` | Remove member |
| `DELETE` | `/workspaces/{id}/leave` | Leave workspace |

### Channels

| Method | Path | Description |
|---|---|---|
| `POST`   | `/workspaces/{wId}/channels` | Create channel |
| `GET`    | `/workspaces/{wId}/channels` | List public channels |
| `GET`    | `/workspaces/{wId}/channels/mine` | List my channels |
| `GET`    | `/workspaces/{wId}/channels/{id}` | Get channel |
| `PATCH`  | `/workspaces/{wId}/channels/{id}` | Update channel |
| `POST`   | `/workspaces/{wId}/channels/{id}/archive` | Archive channel |
| `DELETE` | `/workspaces/{wId}/channels/{id}` | Delete channel |
| `POST`   | `/workspaces/{wId}/channels/{id}/join` | Join channel |
| `DELETE` | `/workspaces/{wId}/channels/{id}/leave` | Leave channel |
| `GET`    | `/workspaces/{wId}/channels/{id}/members` | List channel members |
| `POST`   | `/workspaces/{wId}/channels/{id}/members/{userId}` | Add member |
| `DELETE` | `/workspaces/{wId}/channels/{id}/members/{userId}` | Remove member |
| `PATCH`  | `/workspaces/{wId}/channels/{id}/notifications` | Set notification pref |
| `POST`   | `/workspaces/{wId}/channels/dm` | Create DM or Group DM |

### Messages

| Method | Path | Description |
|---|---|---|
| `POST`   | `/channels/{id}/messages` | Send message (or thread reply) |
| `GET`    | `/channels/{id}/messages?page=0&size=50` | Paginated messages (newest first) |
| `PATCH`  | `/channels/{id}/messages/{msgId}` | Edit message |
| `DELETE` | `/channels/{id}/messages/{msgId}` | Soft-delete message |
| `GET`    | `/channels/{id}/messages/{parentId}/replies` | Get thread replies |
| `POST`   | `/channels/{id}/messages/{msgId}/reactions` | Add reaction |
| `DELETE` | `/channels/{id}/messages/{msgId}/reactions/{reaction}` | Remove reaction |
| `POST`   | `/channels/{id}/messages/read` | Mark channel as read |

### Files

| Method | Path | Description |
|---|---|---|
| `POST`   | `/workspaces/{wId}/files/upload-url` | Get presigned S3 upload URL |
| `POST`   | `/workspaces/{wId}/files/confirm` | Confirm upload complete |
| `GET`    | `/workspaces/{wId}/files` | List workspace files |
| `GET`    | `/workspaces/{wId}/files/{id}` | Get file + download URL |
| `DELETE` | `/workspaces/{wId}/files/{id}` | Delete file |

### Notifications

| Method | Path | Description |
|---|---|---|
| `GET`  | `/notifications` | List my notifications |
| `GET`  | `/notifications/unread-count` | Get unread count |
| `POST` | `/notifications/{id}/read` | Mark one as read |
| `POST` | `/notifications/read-all` | Mark all as read |

### Presence

| Method | Path | Description |
|---|---|---|
| `POST` | `/workspaces/{wId}/presence/heartbeat` | Send online heartbeat |
| `GET`  | `/workspaces/{wId}/presence?userIds=1,2,3` | Bulk presence check |
| `GET`  | `/channels/{id}/typing` | Get typing users |

---

## WebSocket / Real-Time

Connect via SockJS: `ws://localhost:8080/ws`

**STOMP CONNECT frame must include:**
```
Authorization: Bearer <access_token>
```

### Subscribe Topics

| Topic | Payload | Description |
|---|---|---|
| `/topic/channels/{channelId}` | `MessageEvent` | New / edited / deleted messages |
| `/topic/channels/{channelId}/typing` | `{userId, typing}` | Typing indicators |
| `/topic/workspaces/{workspaceId}/presence` | `{userId, online}` | Presence updates |

### Send Destinations (client → server)

| Destination | Description |
|---|---|
| `/app/channels/{channelId}/typing/start` | Start typing indicator |
| `/app/channels/{channelId}/typing/stop` | Stop typing indicator |

### JavaScript Client Example

```javascript
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

const client = new Client({
  webSocketFactory: () => new SockJS("http://localhost:8080/ws"),
  connectHeaders: {
    Authorization: `Bearer ${accessToken}`,
  },
  onConnect: () => {
    // Subscribe to channel messages
    client.subscribe(`/topic/channels/${channelId}`, (frame) => {
      const event = JSON.parse(frame.body);
      console.log("New message event:", event);
    });

    // Subscribe to typing indicators
    client.subscribe(`/topic/channels/${channelId}/typing`, (frame) => {
      const { userId, typing } = JSON.parse(frame.body);
      updateTypingIndicator(userId, typing);
    });
  },
});

client.activate();

// Send a typing indicator
function startTyping() {
  client.publish({ destination: `/app/channels/${channelId}/typing/start` });
}
```

---

## Authentication Flow

```
Register / Login
      │
      ▼
API returns { accessToken (1h), refreshToken (7d) }
      │
      ▼
Client sends: Authorization: Bearer <accessToken>
      │
      ├── Token valid → request proceeds
      │
      └── Token expired → POST /auth/refresh { refreshToken }
                                │
                                ▼
                        New { accessToken, refreshToken }
                        (old refresh token is revoked — rotation)
```

**Security features:**
- Refresh token rotation (old token revoked on each use)
- Reuse detection: if a revoked token is presented, ALL tokens for that user are revoked
- Tokens stored as SHA-256 hashes in the database (not plaintext)
- Automatic nightly cleanup of expired/revoked tokens

---

## File Upload Flow

```
1. Client → POST /workspaces/{id}/files/upload-url
           { fileName, mimeType, fileSize }
           ← { fileId, uploadUrl (presigned S3 PUT), storageKey }

2. Client → PUT {uploadUrl}   (direct to S3, no server proxy)
           Body: raw file bytes
           Headers: Content-Type: image/jpeg

3. Client → POST /workspaces/{id}/files/confirm?storageKey=...
           ← { fileResponse with presigned download URL }

4. Client → POST /channels/{id}/messages
           { content: "See attachment", fileIds: [fileId] }
```

---

## Running Tests

```bash
# Unit tests only (fast, no infrastructure needed)
mvn test

# All tests including integration (requires Docker)
mvn verify

# Specific test class
mvn test -Dtest=AuthServiceImplTest

# With coverage report
mvn test jacoco:report
# Report at: target/site/jacoco/index.html
```

---

## Design Decisions

### Why presigned S3 URLs?
Files are uploaded **directly** from the client to S3, bypassing the API server. This eliminates the server as a bottleneck for large files and reduces server memory pressure.

### Why soft-delete for messages?
Deleted messages show `[deleted]` to preserve thread context and audit trails. Hard deletes would break reply chains.

### Why Kafka instead of direct WebSocket fan-out?
Kafka decouples the write path from the delivery path. Multiple API instances can each consume events independently, enabling horizontal scaling without a shared WebSocket state.

### Why refresh token rotation?
Each refresh produces a new token pair and revokes the old one. If a token is stolen and used, the legitimate user's next refresh will detect the conflict (revoked token presented) and wipe all sessions — protecting the account.

### Why MapStruct over manual mapping?
MapStruct generates compile-time, type-safe mapping code with zero reflection overhead. It eliminates entire classes of `NullPointerException` and mapping bugs caught at compile time, not runtime.

### Why Records for DTOs?
Java records are immutable, concise, and have built-in `equals`, `hashCode`, and `toString`. They're ideal for DTOs where you want value semantics and no accidental mutation.

---

## Production Checklist

- [ ] **Change `JWT_SECRET`** to a securely generated 256-bit key
- [ ] Enable **HTTPS** (TLS termination at load balancer)
- [ ] Use **RDS Multi-AZ** for MySQL
- [ ] Use **ElastiCache** for Redis (replication group)
- [ ] Use **MSK** for Kafka (managed, multi-broker)
- [ ] Configure **S3 bucket policies** and enable server-side encryption
- [ ] Set up **Flyway** baseline migration on existing databases
- [ ] Enable **Spring Security method-level annotations** for fine-grained access
- [ ] Configure **rate limiting** at API Gateway / WAF level
- [ ] Set up **Prometheus + Grafana** (actuator/prometheus endpoint is exposed)
- [ ] Enable **structured JSON logging** (`logback-spring.xml` with ECS layout)
- [ ] Add **OpenSearch** indexer consumer for full-text message search
- [ ] Implement **virus scanning webhook** from S3 → Lambda → API `/files/scan-result`
- [ ] Enable **email verification** on registration (token table is already in schema)
- [ ] Set up **dead-letter queues** for Kafka consumer failures
