#!/bin/bash

# Swagger Setup Script for Meeting Room Booking API

echo "=========================================="
echo "Swagger/OpenAPI Documentation Setup"
echo "=========================================="
echo ""

# Check if Go is installed
if ! command -v go &> /dev/null; then
    echo "❌ Error: Go is not installed"
    exit 1
fi

echo "✅ Go is installed"

# Install Swagger CLI
echo ""
echo "📦 Installing Swagger CLI (swag)..."
go install github.com/swaggo/swag/cmd/swag@latest

if ! command -v swag &> /dev/null; then
    echo "⚠️  Warning: swag command not found. Make sure \$GOPATH/bin is in your PATH"
    echo "   Add to your ~/.zshrc or ~/.bashrc:"
    echo "   export PATH=\$PATH:\$(go env GOPATH)/bin"
    exit 1
fi

echo "✅ Swagger CLI installed successfully"

# Install Go dependencies
echo ""
echo "📦 Installing Go dependencies..."
go mod tidy

if [ $? -ne 0 ]; then
    echo "❌ Error: Failed to install dependencies"
    exit 1
fi

echo "✅ Dependencies installed successfully"

# Generate Swagger documentation
echo ""
echo "📝 Generating Swagger documentation..."
swag init -g cmd/server/main.go -o docs

if [ $? -ne 0 ]; then
    echo "❌ Error: Failed to generate Swagger docs"
    exit 1
fi

echo "✅ Swagger documentation generated successfully"

# Summary
echo ""
echo "=========================================="
echo "✨ Setup Complete!"
echo "=========================================="
echo ""
echo "📚 Documentation files created in: docs/"
echo ""
echo "🚀 To start the server:"
echo "   go run cmd/server/main.go"
echo ""
echo "📖 Access Swagger UI at:"
echo "   http://localhost:8080/swagger/index.html"
echo ""
echo "🔑 API Tags (matching Java implementation):"
echo "   • Authentication - User registration and login"
echo "   • Meeting Rooms - Room management endpoints"
echo "   • Bookings - Booking management endpoints"
echo "   • Dashboard - Dashboard statistics"
echo ""
echo "💡 To regenerate docs after changes:"
echo "   make swagger"
echo "   or"
echo "   swag init -g cmd/server/main.go -o docs"
echo ""
