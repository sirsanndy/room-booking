.PHONY: build run test clean docker-build docker-run install

# Build the application
build:
	@echo "Building server..."
	go build -o bin/server cmd/server/main.go

# Run the application
run:
	@echo "Running server..."
	go run cmd/server/main.go

# Run tests
test:
	@echo "Running tests..."
	go test -v ./...

# Run tests with coverage
test-coverage:
	@echo "Running tests with coverage..."
	go test -v -coverprofile=coverage.out ./...
	go tool cover -html=coverage.out -o coverage.html
	@echo "Coverage report generated: coverage.html"

# Clean build artifacts
clean:
	@echo "Cleaning..."
	rm -rf bin/
	rm -f coverage.out coverage.html

# Install dependencies
install:
	@echo "Installing dependencies..."
	go mod download
	go mod tidy

# Install Swagger CLI
install-swagger:
	@echo "Installing Swagger CLI..."
	go install github.com/swaggo/swag/cmd/swag@latest

# Generate Swagger documentation
swagger:
	@echo "Generating Swagger documentation..."
	swag init -g cmd/server/main.go -o docs
	@echo "Swagger docs generated in docs/ directory"
	@echo "Access at: http://localhost:8080/swagger/index.html"

# Format code
fmt:
	@echo "Formatting code..."
	go fmt ./...

# Run linter
lint:
	@echo "Running linter..."
	golangci-lint run

# Build Docker image
docker-build:
	@echo "Building Docker image..."
	docker build -t meeting-room-booking:latest .

# Run Docker container
docker-run:
	@echo "Running Docker container..."
	docker run -p 8080:8080 --env-file .env meeting-room-booking:latest

# Run with Docker Compose
docker-compose-up:
	@echo "Starting services with Docker Compose..."
	docker-compose up -d

docker-compose-down:
	@echo "Stopping services..."
	docker-compose down

# Database migrations
db-migrate:
	@echo "Running database migrations..."
	psql -U postgres -d meetingroom_db -f migrations/schema.sql

# Create database
db-create:
	@echo "Creating database..."
	psql -U postgres -c "CREATE DATABASE meetingroom_db;"

# Drop database
db-drop:
	@echo "Dropping database..."
	psql -U postgres -c "DROP DATABASE IF EXISTS meetingroom_db;"

# Reset database
db-reset: db-drop db-create db-migrate
	@echo "Database reset complete!"

# Generate mocks (requires mockgen)
mocks:
	@echo "Generating mocks..."
	mockgen -source=internal/repository/user_repository.go -destination=internal/repository/mocks/user_repository_mock.go
	mockgen -source=internal/repository/room_repository.go -destination=internal/repository/mocks/room_repository_mock.go
	mockgen -source=internal/repository/booking_repository.go -destination=internal/repository/mocks/booking_repository_mock.go

# Show help
help:
	@echo "Available commands:"
	@echo "  make build              - Build the application"
	@echo "  make run                - Run the application"
	@echo "  make test               - Run tests"
	@echo "  make test-coverage      - Run tests with coverage"
	@echo "  make clean              - Clean build artifacts"
	@echo "  make install            - Install dependencies"
	@echo "  make fmt                - Format code"
	@echo "  make lint               - Run linter"
	@echo "  make docker-build       - Build Docker image"
	@echo "  make docker-run         - Run Docker container"
	@echo "  make docker-compose-up  - Start with Docker Compose"
	@echo "  make docker-compose-down- Stop Docker Compose"
	@echo "  make db-create          - Create database"
	@echo "  make db-migrate         - Run migrations"
	@echo "  make db-reset           - Reset database"
	@echo "  make mocks              - Generate mocks"
