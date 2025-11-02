# Quick Start Guide - Backend Reactive

## Prerequisites Check

```bash
# Check Java version (must be 21)
java -version

# Check PostgreSQL is running
psql -U postgres -c "SELECT version();"

# Check Redis is running
redis-cli ping
# Should return: PONG
```

## Step 1: Navigate to Project

```bash
cd backend-reactive
```

## Step 2: Build the Project

```bash
# Clean and build (skip tests for faster build)
./mvnw clean package -DskipTests

# Or with tests
./mvnw clean package
```

Expected output:
```
[INFO] BUILD SUCCESS
[INFO] Total time: 45 s
```

## Step 3: Run the Application

```bash
# Option 1: Run with Maven
./mvnw spring-boot:run

# Option 2: Run the JAR
java -jar target/booking-backend-reactive-1.0.0.jar
```

Wait for:
```
Started BookingReactiveApplication in X.XXX seconds
```

## Step 4: Verify Application is Running

```bash
# Health check
curl http://localhost:8081/actuator/health

# Expected: {"status":"UP"}
```

## Step 5: Test the API

### 1. Register a User

```bash
curl -X POST http://localhost:8081/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john",
    "password": "password123",
    "email": "john@example.com",
    "fullName": "John Doe"
  }'
```

Expected: `{"message":"User registered successfully"}`

### 2. Login

```bash
curl -X POST http://localhost:8081/api/auth/signin \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john",
    "password": "password123"
  }'
```

Save the token from response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "id": 1,
  "username": "john",
  "email": "john@example.com",
  "roles": ["USER"]
}
```

### 3. Get Available Rooms

```bash
curl http://localhost:8081/api/rooms/available
```

### 4. Create a Booking

```bash
# Replace YOUR_TOKEN_HERE with actual token
curl -X POST http://localhost:8081/api/bookings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{
    "roomId": 1,
    "startTime": "2025-11-03T09:00:00",
    "endTime": "2025-11-03T11:00:00",
    "title": "Team Meeting",
    "description": "Weekly sync"
  }'
```

### 5. View Your Bookings

```bash
curl http://localhost:8081/api/bookings/my-bookings \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

## Common Issues & Solutions

### Issue 1: Port Already in Use

```
Error: Port 8081 already in use
```

**Solution**: Change port in `application.properties`:
```properties
server.port=8082
```

### Issue 2: Database Connection Failed

```
Error: Connection refused: localhost:5432
```

**Solution**: Start PostgreSQL:
```bash
# macOS with Homebrew
brew services start postgresql@14

# Linux
sudo systemctl start postgresql

# Check if running
psql -U postgres -c "SELECT 1"
```

### Issue 3: Redis Connection Failed

```
Error: Connection refused: localhost:6379
```

**Solution**: Start Redis:
```bash
# macOS with Homebrew
brew services start redis

# Linux
sudo systemctl start redis

# Check if running
redis-cli ping
```

### Issue 4: Build Failed - Java Version

```
Error: error: invalid target release: 21
```

**Solution**: Install JDK 21:
```bash
# macOS with SDKMAN
sdk install java 21.0.1-tem
sdk use java 21.0.1-tem

# Verify
java -version
```

### Issue 5: Database Schema Not Found

```
Error: relation "users" does not exist
```

**Solution**: The database should already be created by the original backend. If not, create it:
```sql
-- Connect to PostgreSQL
psql -U postgres

-- Create database
CREATE DATABASE meetingroom_db;
```

Then run the original backend once to create tables, or create them manually.

## Environment Variables

You can override configuration with environment variables:

```bash
# Database
export SPRING_R2DBC_URL=r2dbc:postgresql://localhost:5432/meetingroom_db
export SPRING_R2DBC_USERNAME=postgres
export SPRING_R2DBC_PASSWORD=your_password

# Redis
export SPRING_DATA_REDIS_HOST=localhost
export SPRING_DATA_REDIS_PORT=6379

# JWT
export JWT_SECRET=your_secret_key
export JWT_EXPIRATION=86400000

# Run application
java -jar target/booking-backend-reactive-1.0.0.jar
```

## Docker Quick Start

### Build and Run with Docker

```bash
# Build the application
./mvnw clean package -DskipTests

# Build Docker image
docker build -t booking-reactive .

# Run container
docker run -d \
  --name booking-reactive \
  -p 8081:8081 \
  -e SPRING_R2DBC_URL=r2dbc:postgresql://host.docker.internal:5432/meetingroom_db \
  -e SPRING_DATA_REDIS_HOST=host.docker.internal \
  booking-reactive

# Check logs
docker logs -f booking-reactive

# Stop container
docker stop booking-reactive

# Remove container
docker rm booking-reactive
```

## Running Both Backends Simultaneously

You can run both the original and reactive backends at the same time:

```bash
# Terminal 1: Original Backend
cd backend
./mvnw spring-boot:run
# Runs on http://localhost:8080

# Terminal 2: Reactive Backend
cd backend-reactive
./mvnw spring-boot:run
# Runs on http://localhost:8081
```

Both use the same database and Redis instance.

## Monitoring

### Health Check

```bash
curl http://localhost:8081/actuator/health
```

### Prometheus Metrics

```bash
curl http://localhost:8081/actuator/prometheus
```

### View Application Info

```bash
curl http://localhost:8081/actuator/info
```

## Development Tips

### Hot Reload with Spring Boot DevTools

Add to `pom.xml` (optional):
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <optional>true</optional>
</dependency>
```

### Enable Debug Logging

Edit `application.properties`:
```properties
logging.level.com.meetingroom.booking=DEBUG
logging.level.org.springframework.data.r2dbc=DEBUG
```

### View SQL Queries

Already enabled in `application.properties`:
```properties
logging.level.io.r2dbc.postgresql.QUERY=DEBUG
```

## Performance Testing

### Simple Load Test with Apache Bench

```bash
# Install Apache Bench
# macOS: already installed
# Ubuntu: sudo apt-get install apache2-utils

# Test login endpoint
ab -n 1000 -c 10 -T 'application/json' \
  -p login.json \
  http://localhost:8081/api/auth/signin
```

Where `login.json` contains:
```json
{
  "username": "john",
  "password": "password123"
}
```

## Stopping the Application

```bash
# If running with Maven
# Press Ctrl+C in the terminal

# If running as JAR, find and kill process
# macOS/Linux
lsof -ti:8081 | xargs kill -9

# Or more gracefully
kill $(lsof -ti:8081)
```

## Next Steps

1. ✅ Application running successfully
2. ✅ Tested basic CRUD operations
3. Read `IMPLEMENTATION-SUMMARY.md` for architecture details
4. Read `ARCHITECTURE-DIAGRAM.md` for visual architecture
5. Explore the code in your IDE
6. Run tests: `./mvnw test`
7. Add your own features!

## Getting Help

- Check logs in `logs/application.log`
- Review `README.md` for detailed information
- Check `application.properties` for configuration
- All errors are logged with stack traces

## Success Indicators

✅ Application starts without errors
✅ Health check returns "UP"
✅ Can register new user
✅ Can login and receive JWT token
✅ Can create booking with valid token
✅ Can view bookings
✅ Prometheus metrics accessible

Congratulations! Your reactive backend is now running! 🎉
