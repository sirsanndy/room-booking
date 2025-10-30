#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}   Meeting Room Booking - Production Deployment${NC}"
echo -e "${BLUE}════════════════════════════════════════════════════════${NC}"
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${YELLOW}⚠️  Docker is not running. Please start Docker first.${NC}"
    exit 1
fi

echo -e "${GREEN}✓${NC} Docker is running"
echo ""

# Build and start all services
echo -e "${BLUE}Building and starting all services...${NC}"
echo ""

docker-compose up -d --build

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}✓ All services started successfully!${NC}"
    echo -e "${GREEN}════════════════════════════════════════════════════════${NC}"
    echo ""
    echo -e "${BLUE}Services:${NC}"
    echo -e "  📱 Frontend:    ${GREEN}http://localhost${NC}"
    echo -e "  🔧 Backend:     ${GREEN}http://localhost:8080${NC}"
    echo -e "  🗄️  PostgreSQL:  ${GREEN}localhost:5432${NC}"
    echo -e "  📦 Redis:       ${GREEN}localhost:6379${NC}"
    echo -e "  📊 Prometheus:  ${GREEN}http://localhost:9090${NC}"
    echo -e "  📈 Grafana:     ${GREEN}http://localhost:3001${NC} (admin/admin)"
    echo ""
    echo -e "${BLUE}Useful commands:${NC}"
    echo -e "  View logs:      ${YELLOW}docker-compose logs -f${NC}"
    echo -e "  Stop services:  ${YELLOW}docker-compose down${NC}"
    echo -e "  Restart:        ${YELLOW}docker-compose restart${NC}"
    echo ""
    echo -e "${BLUE}Check service status:${NC}"
    docker-compose ps
    echo ""
else
    echo -e "${YELLOW}⚠️  Failed to start services. Check the logs:${NC}"
    echo -e "${YELLOW}docker-compose logs${NC}"
    exit 1
fi
