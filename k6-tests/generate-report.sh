#!/bin/bash

###############################################################################
# Generate HTML Report from K6 JSON Results
# Usage: ./generate-report.sh [results-dir]
###############################################################################

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  K6 HTML Report Generator                                 ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Get results directory
if [ -z "$1" ]; then
  # Find the latest results directory
  RESULTS_DIR=$(ls -td ./results/*/ 2>/dev/null | head -1)
  if [ -z "$RESULTS_DIR" ]; then
    echo -e "${RED}❌ No results directory found${NC}"
    echo -e "${YELLOW}Usage: $0 [results-dir]${NC}"
    echo -e "${YELLOW}Example: $0 ./results/20231029_120000${NC}"
    exit 1
  fi
  echo -e "${YELLOW}📂 Using latest results: $RESULTS_DIR${NC}"
else
  RESULTS_DIR="$1"
  if [ ! -d "$RESULTS_DIR" ]; then
    echo -e "${RED}❌ Results directory not found: $RESULTS_DIR${NC}"
    exit 1
  fi
fi

# Remove trailing slash
RESULTS_DIR="${RESULTS_DIR%/}"

# Check for JSON files
JSON_COUNT=$(find "$RESULTS_DIR" -name "*.json" -type f 2>/dev/null | wc -l | tr -d ' ')
if [ "$JSON_COUNT" -eq 0 ]; then
  echo -e "${RED}❌ No JSON result files found in: $RESULTS_DIR${NC}"
  exit 1
fi

echo -e "${GREEN}✅ Found $JSON_COUNT test result file(s)${NC}"
echo ""

# Check if Node.js is available
if ! command -v node &> /dev/null; then
  echo -e "${RED}❌ Node.js not found${NC}"
  echo -e "${YELLOW}Please install Node.js: https://nodejs.org${NC}"
  exit 1
fi

# Install dependencies if needed
if [ ! -d "node_modules" ]; then
  echo -e "${YELLOW}📦 Installing dependencies...${NC}"
  npm install --silent
  echo ""
fi

# Generate report
OUTPUT_FILE="$RESULTS_DIR/report.html"
echo -e "${BLUE}📊 Generating HTML report...${NC}"
echo ""

if node scripts/generate-html-report.js "$RESULTS_DIR" "$OUTPUT_FILE"; then
  echo ""
  echo -e "${GREEN}╔════════════════════════════════════════════════════════════╗${NC}"
  echo -e "${GREEN}║  Report Generated Successfully! 🎉                        ║${NC}"
  echo -e "${GREEN}╚════════════════════════════════════════════════════════════╝${NC}"
  echo ""
  echo -e "${BLUE}📄 Report location:${NC}"
  echo -e "   $OUTPUT_FILE"
  echo ""
  echo -e "${BLUE}🌐 View in browser:${NC}"
  echo -e "   open $OUTPUT_FILE"
  echo -e "   or"
  echo -e "   file://$(cd "$(dirname "$OUTPUT_FILE")" && pwd)/$(basename "$OUTPUT_FILE")"
  echo ""
  
  # Try to open in browser
  if command -v open &> /dev/null; then
    read -p "Open report in browser now? (y/n) " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Yy]$ ]]; then
      open "$OUTPUT_FILE"
      echo -e "${GREEN}✅ Report opened in browser${NC}"
    fi
  elif command -v xdg-open &> /dev/null; then
    read -p "Open report in browser now? (y/n) " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Yy]$ ]]; then
      xdg-open "$OUTPUT_FILE"
      echo -e "${GREEN}✅ Report opened in browser${NC}"
    fi
  fi
else
  echo -e "${RED}❌ Failed to generate HTML report${NC}"
  exit 1
fi

echo ""
