#!/bin/bash
# A2A Spring Demo Test Script
# Usage: ./test-a2a.sh [base-url]

BASE_URL="${1:-http://localhost:8080}"

echo "========================================="
echo "A2A Spring Demo - HTTP Test Script"
echo "Base URL: $BASE_URL"
echo "========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to make HTTP request and check status
make_request() {
    local method=$1
    local endpoint=$2
    local data=$3
    local description=$4
    
    echo "Testing: $description"
    echo "Endpoint: $method $endpoint"
    
    if [ -n "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -X "$method" \
            -H "Content-Type: application/json" \
            -d "$data" \
            "$endpoint" 2>&1)
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" \
            "$endpoint" 2>&1)
    fi
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" -eq 200 ]; then
        echo -e "${GREEN}✓ Success (HTTP $http_code)${NC}"
    else
        echo -e "${RED}✗ Failed (HTTP $http_code)${NC}"
    fi
    
    echo "Response:"
    echo "$body" | python3 -m json.tool 2>/dev/null || echo "$body"
    echo ""
    echo "----------------------------------------"
    echo ""
}

# Test 1: Health Check
make_request "GET" "$BASE_URL/health" "" "Health Check"

# Test 2: Agent Card
make_request "GET" "$BASE_URL/.well-known/agent-card.json" "" "Agent Card Discovery"

# Test 3: Send Weather Task
WEATHER_PAYLOAD='{
  "jsonrpc": "2.0",
  "id": "req-001",
  "method": "tasks/send",
  "params": {
    "id": "task-001",
    "message": {
      "role": "user",
      "parts": [
        {
          "kind": "text",
          "text": "What'"'"'s the weather in London?"
        }
      ]
    }
  }
}'
make_request "POST" "$BASE_URL/" "$WEATHER_PAYLOAD" "Send Weather Task"

# Test 4: Get Task Status
TASK_GET_PAYLOAD='{
  "jsonrpc": "2.0",
  "id": "req-002",
  "method": "tasks/get",
  "params": {
    "id": "task-001"
  }
}'
make_request "POST" "$BASE_URL/" "$TASK_GET_PAYLOAD" "Get Task Status"

# Test 5: Send Forecast Task
FORECAST_PAYLOAD='{
  "jsonrpc": "2.0",
  "id": "req-003",
  "method": "tasks/send",
  "params": {
    "id": "task-002",
    "message": {
      "role": "user",
      "parts": [
        {
          "kind": "text",
          "text": "Give me a 3-day forecast for New York"
        }
      ]
    }
  }
}'
make_request "POST" "$BASE_URL/" "$FORECAST_PAYLOAD" "Send Forecast Task"

# Test 6: Unknown Method
UNKNOWN_PAYLOAD='{
  "jsonrpc": "2.0",
  "id": "req-error-001",
  "method": "tasks/unknown",
  "params": {}
}'
make_request "POST" "$BASE_URL/" "$UNKNOWN_PAYLOAD" "Unknown Method (Error Test)"

# Test 7: Invalid JSON-RPC Version
INVALID_VERSION_PAYLOAD='{
  "jsonrpc": "1.0",
  "id": "req-error-002",
  "method": "tasks/send",
  "params": {
    "id": "task-error-001",
    "message": {
      "role": "user",
      "parts": [
        {
          "kind": "text",
          "text": "Hello"
        }
      ]
    }
  }
}'
make_request "POST" "$BASE_URL/" "$INVALID_VERSION_PAYLOAD" "Invalid JSON-RPC Version (Error Test)"

echo ""
echo "========================================="
echo "Testing Complete!"
echo "========================================="

# Note about streaming
echo ""
echo "Note: To test streaming endpoint, use:"
echo "  curl -N -X POST $BASE_URL/stream \\"
echo "    -H 'Content-Type: application/json' \\"
echo "    -H 'Accept: text/event-stream' \\"
echo "    -d '{\"jsonrpc\":\"2.0\",\"id\":\"stream-1\",\"method\":\"tasks/send\",\"params\":{\"id\":\"task-stream-1\",\"message\":{\"role\":\"user\",\"parts\":[{\"kind\":\"text\",\"text\":\"What'"'"'s the weather in Tokyo?\"}]}}}'"
