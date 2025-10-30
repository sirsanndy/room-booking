#!/bin/bash

# Observability Stack Startup Script for Meeting Room Booking Application
# This script starts all monitoring infrastructure and provides access URLs

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Print colored output
print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Print header
echo ""
echo "╔════════════════════════════════════════════════════╗"
echo "║  Meeting Room Booking - Observability Stack       ║"
echo "║  Starting Prometheus, Grafana, and Exporters      ║"
echo "╚════════════════════════════════════════════════════╝"
echo ""

# Check if Docker is running
print_info "Checking Docker status..."
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker and try again."
    exit 1
fi
print_success "Docker is running"

# Check if docker-compose.yml exists
if [ ! -f "docker-compose.yml" ]; then
    print_error "docker-compose.yml not found. Please run this script from the project root."
    exit 1
fi

# Stop and remove existing containers (if any)
print_info "Cleaning up existing containers..."
docker-compose down > /dev/null 2>&1 || true
print_success "Cleanup complete"

# Start infrastructure services
print_info "Starting infrastructure services..."
echo ""

docker-compose up -d postgres
print_success "PostgreSQL started (port 5432)"
sleep 3

docker-compose up -d redis
print_success "Redis started (port 6379)"
sleep 2

# Start monitoring services
print_info "Starting monitoring services..."
echo ""

docker-compose up -d postgres-exporter
print_success "PostgreSQL Exporter started (port 9187)"
sleep 2

docker-compose up -d redis-exporter
print_success "Redis Exporter started (port 9121)"
sleep 2

docker-compose up -d prometheus
print_success "Prometheus started (port 9090)"
sleep 3

docker-compose up -d grafana
print_success "Grafana started (port 3001)"
sleep 5

# Check service health
print_info "Checking service health..."
echo ""

check_service() {
    local service=$1
    local port=$2
    local max_attempts=30
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        if curl -s "http://localhost:$port" > /dev/null 2>&1; then
            return 0
        fi
        sleep 1
        attempt=$((attempt + 1))
    done
    return 1
}

# Check PostgreSQL
if docker exec meetingroom-postgres pg_isready -U postgres > /dev/null 2>&1; then
    print_success "PostgreSQL is healthy"
else
    print_warning "PostgreSQL may not be ready yet"
fi

# Check Redis
if docker exec meetingroom-redis redis-cli ping > /dev/null 2>&1; then
    print_success "Redis is healthy"
else
    print_warning "Redis may not be ready yet"
fi

# Check Prometheus
if check_service "Prometheus" 9090; then
    print_success "Prometheus is healthy"
else
    print_warning "Prometheus may not be ready yet"
fi

# Check Grafana
if check_service "Grafana" 3001; then
    print_success "Grafana is healthy"
else
    print_warning "Grafana may not be ready yet"
fi

# Print access information
echo ""
echo "╔════════════════════════════════════════════════════╗"
echo "║  🎉 Observability Stack Started Successfully!     ║"
echo "╚════════════════════════════════════════════════════╝"
echo ""
print_info "Access URLs:"
echo ""
echo "  📊 Grafana Dashboard:"
echo "     URL:      http://localhost:3001"
echo "     Username: admin"
echo "     Password: admin"
echo ""
echo "  🔍 Prometheus:"
echo "     URL:      http://localhost:9090"
echo ""
echo "  📈 Metrics Endpoints:"
echo "     App:      http://localhost:8080/actuator/prometheus"
echo "     Postgres: http://localhost:9187/metrics"
echo "     Redis:    http://localhost:9121/metrics"
echo ""
echo "  💾 Databases:"
echo "     PostgreSQL: localhost:5432 (user: postgres, pass: postgres)"
echo "     Redis:      localhost:6379"
echo ""

# Print next steps
print_info "Next Steps:"
echo ""
echo "  1. Start the Spring Boot application:"
echo "     cd backend && ./mvnw spring-boot:run"
echo ""
echo "  2. Open Grafana and view the dashboard:"
echo "     http://localhost:3001"
echo ""
echo "  3. View Prometheus metrics:"
echo "     http://localhost:9090"
echo ""
echo "  4. Generate some traffic to see metrics:"
echo "     - Open the frontend: http://localhost:5173"
echo "     - Create some bookings"
echo "     - Check the Grafana dashboard"
echo ""

print_info "To view logs:"
echo "  docker-compose logs -f [service-name]"
echo ""
print_info "To stop all services:"
echo "  docker-compose down"
echo ""

print_success "Setup complete! Happy monitoring! 📊"
echo ""
