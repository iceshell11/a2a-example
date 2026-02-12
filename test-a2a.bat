@echo off
REM A2A Spring Demo Test Script for Windows
REM Usage: test-a2a.bat [base-url]

set BASE_URL=%~1
if "%BASE_URL%"=="" set BASE_URL=http://localhost:8080

echo =========================================
echo A2A Spring Demo - HTTP Test Script
echo Base URL: %BASE_URL%
echo =========================================
echo.

REM Test 1: Health Check
echo Testing: Health Check
echo Endpoint: GET %BASE_URL%/health
curl -s -X GET "%BASE_URL%/health" -H "Accept: application/json"
echo.
echo ----------------------------------------
echo.

REM Test 2: Agent Card
echo Testing: Agent Card Discovery
echo Endpoint: GET %BASE_URL%/.well-known/agent-card.json
curl -s -X GET "%BASE_URL%/.well-known/agent-card.json" -H "Accept: application/json"
echo.
echo ----------------------------------------
echo.

REM Test 3: Send Weather Task
echo Testing: Send Weather Task
echo Endpoint: POST %BASE_URL%/
curl -s -X POST "%BASE_URL%/" ^
  -H "Content-Type: application/json" ^
  -d "{\"jsonrpc\":\"2.0\",\"id\":\"req-001\",\"method\":\"tasks/send\",\"params\":{\"id\":\"task-001\",\"message\":{\"role\":\"user\",\"parts\":[{\"kind\":\"text\",\"text\":\"What's the weather in London?\"}]}}}"
echo.
echo ----------------------------------------
echo.

REM Test 4: Send Forecast Task
echo Testing: Send Forecast Task
echo Endpoint: POST %BASE_URL%/
curl -s -X POST "%BASE_URL%/" ^
  -H "Content-Type: application/json" ^
  -d "{\"jsonrpc\":\"2.0\",\"id\":\"req-002\",\"method\":\"tasks/send\",\"params\":{\"id\":\"task-002\",\"message\":{\"role\":\"user\",\"parts\":[{\"kind\":\"text\",\"text\":\"Give me a 3-day forecast for New York\"}]}}}"
echo.
echo ----------------------------------------
echo.

REM Test 5: Unknown Method (Error Test)
echo Testing: Unknown Method (Error Test)
echo Endpoint: POST %BASE_URL%/
curl -s -X POST "%BASE_URL%/" ^
  -H "Content-Type: application/json" ^
  -d "{\"jsonrpc\":\"2.0\",\"id\":\"req-error-001\",\"method\":\"tasks/unknown\",\"params\":{}}"
echo.
echo ----------------------------------------
echo.

REM Test 6: Invalid JSON-RPC Version (Error Test)
echo Testing: Invalid JSON-RPC Version (Error Test)
echo Endpoint: POST %BASE_URL%/
curl -s -X POST "%BASE_URL%/" ^
  -H "Content-Type: application/json" ^
  -d "{\"jsonrpc\":\"1.0\",\"id\":\"req-error-002\",\"method\":\"tasks/send\",\"params\":{\"id\":\"task-error-001\",\"message\":{\"role\":\"user\",\"parts\":[{\"kind\":\"text\",\"text\":\"Hello\"}]}}}"
echo.
echo ----------------------------------------
echo.

echo.
echo =========================================
echo Testing Complete!
echo =========================================
echo.
echo Note: To test streaming endpoint, use:
echo   curl -N -X POST %BASE_URL%/stream ^
echo     -H "Content-Type: application/json" ^
echo     -H "Accept: text/event-stream" ^
echo     -d "{\"jsonrpc\":\"2.0\",\"id\":\"stream-1\",\"method\":\"tasks/send\",\"params\":{\"id\":\"task-stream-1\",\"message\":{\"role\":\"user\",\"parts\":[{\"kind\":\"text\",\"text\":\"What's the weather in Tokyo?\"}]}}}"
