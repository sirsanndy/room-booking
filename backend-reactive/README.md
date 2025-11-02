# Meeting Room Booking Backend - Reactive

A reactive implementation of the Meeting Room Booking system using **Spring Boot 3**, **Hexagonal Architecture**, **WebFlux**, and **R2DBC**.

## Architecture

This project follows **Hexagonal Architecture** (Ports and Adapters) with the following layers:

### Domain Layer (`domain`)
- **Models**: Core business entities (`User`, `Booking`, `MeetingRoom`, `Holiday`)
- **Ports (In)**: Use case interfaces defining business operations
- **Ports (Out)**: Repository and infrastructure interfaces
- **Services**: Business logic implementation

### Infrastructure Layer (`infrastructure`)
- **Persistence**: R2DBC repository adapters for PostgreSQL
- **Cache**: Redis adapter for caching
- **Rate Limiter**: Redis-based rate limiting
- **Config**: Security, Redis, R2DBC configurations

### Application Layer (`application`)
- **REST Controllers**: Reactive REST API endpoints
- **DTOs**: Data Transfer Objects for API requests/responses
- **Exception Handlers**: Global exception handling

## Key Features

- ✅ **Reactive Programming**: Non-blocking I/O with Project Reactor
- ✅ **Hexagonal Architecture**: Clean separation of concerns
- ✅ **Spring Boot 3**: Latest Spring framework with JDK 21
- ✅ **R2DBC**: Reactive database access with PostgreSQL
- ✅ **Redis Caching**: Reactive caching for improved performance
- ✅ **JWT Authentication**: Stateless authentication
- ✅ **Rate Limiting**: Prevent API abuse
- ✅ **Constructor Injection**: No field injection, no Lombok
- ✅ **Validation**: Business rules enforcement
- ✅ **Prometheus Metrics**: Observable application

## Technology Stack

- **Java 21**
- **Spring Boot 3.5.7**
- **Spring WebFlux** (Reactive Web)
- **Spring Data R2DBC** (Reactive Database)
- **Spring Security** (Reactive)
- **PostgreSQL** with R2DBC Driver
- **Redis** (Reactive)
- **JWT** for authentication
- **Maven** for build

## Prerequisites

- JDK 21
- PostgreSQL 12+
- Redis 6+
- Maven 3.8+

## Database Setup

The application uses the same database as the original project. Ensure you have:

```sql
-- The database and tables should already exist from the original project
-- This reactive version uses the same schema
```

## Configuration

Edit `src/main/resources/application.properties`:

```properties
# Server runs on port 8081 (different from original)
server.port=8081

# R2DBC PostgreSQL
spring.r2dbc.url=r2dbc:postgresql://localhost:5432/meetingroom_db
spring.r2dbc.username=postgres
spring.r2dbc.password=postgres

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

## Building

```bash
# Clean and build
./mvnw clean package

# Skip tests
./mvnw clean package -DskipTests
```

## Running

```bash
# Run with Maven
./mvnw spring-boot:run

# Or run the JAR
java -jar target/booking-backend-reactive-1.0.0.jar
```

The application will start on **http://localhost:8081**

## API Endpoints

### Authentication
- `POST /api/auth/signup` - Register new user
- `POST /api/auth/signin` - Login

### Rooms
- `GET /api/rooms` - Get all rooms
- `GET /api/rooms/available` - Get available rooms
- `GET /api/rooms/{id}` - Get room by ID

### Bookings (Requires Authentication)
- `POST /api/bookings` - Create booking
- `GET /api/bookings` - Get all upcoming bookings
- `GET /api/bookings/room/{roomId}` - Get bookings by room
- `GET /api/bookings/my-bookings` - Get user's bookings
- `DELETE /api/bookings/{id}` - Cancel booking

### Metrics
- `GET /actuator/health` - Health check
- `GET /actuator/prometheus` - Prometheus metrics

## Docker

Build and run with Docker:

```bash
# Build the application
./mvnw clean package

# Build Docker image
docker build -t booking-backend-reactive .

# Run container
docker run -p 8081:8081 \
  -e SPRING_R2DBC_URL=r2dbc:postgresql://host.docker.internal:5432/meetingroom_db \
  -e SPRING_DATA_REDIS_HOST=host.docker.internal \
  booking-backend-reactive
```

## Architecture Highlights

### Hexagonal Architecture Benefits
1. **Domain Independence**: Business logic doesn't depend on frameworks
2. **Testability**: Easy to mock ports and test use cases
3. **Flexibility**: Easy to swap implementations (e.g., different database)
4. **Clear Boundaries**: Well-defined interfaces between layers

### Reactive Benefits
1. **Non-blocking**: Better resource utilization
2. **Scalability**: Handle more concurrent requests
3. **Backpressure**: Automatic flow control
4. **Composable**: Easy to combine operations

## Project Structure

```
backend-reactive/
├── src/main/java/com/meetingroom/booking/
│   ├── BookingReactiveApplication.java
│   ├── domain/
│   │   ├── model/                    # Domain entities
│   │   ├── port/
│   │   │   ├── in/                   # Use case interfaces
│   │   │   └── out/                  # Repository interfaces
│   │   └── service/                  # Business logic
│   ├── infrastructure/
│   │   ├── persistence/              # R2DBC adapters
│   │   ├── cache/                    # Redis cache adapter
│   │   ├── ratelimiter/              # Rate limiter adapter
│   │   ├── config/                   # Configuration
│   │   └── exception/                # Exception handlers
│   └── application/
│       ├── rest/                     # REST controllers
│       └── dto/                      # DTOs
└── src/main/resources/
    └── application.properties
```

## Differences from Original

1. **Reactive Stack**: WebFlux instead of Web MVC
2. **R2DBC**: Instead of JPA/Hibernate
3. **Hexagonal Architecture**: Clear ports and adapters
4. **Constructor Injection**: No field injection
5. **No Lombok**: Explicit getters/setters
6. **Port 8081**: Different port to run alongside original

## Testing

Run tests:

```bash
./mvnw test
```

## Contributing

This is a reactive rewrite maintaining the same functionality as the original project while demonstrating hexagonal architecture and reactive programming patterns.

## License

Same as the original project.
