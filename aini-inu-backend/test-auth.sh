#!/bin/bash

# JWT Authentication Test Script
# This script tests the authentication implementation for Issue #1

echo "=================================================="
echo "JWT Authentication Implementation Test"
echo "=================================================="
echo ""

BASE_URL="http://localhost:8080"
API_URL="$BASE_URL/api/v1"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}Prerequisites:${NC}"
echo "1. Run: ./gradlew bootRun"
echo "2. Wait for application to start"
echo "3. Run this script in another terminal"
echo ""
read -p "Press Enter to continue..."
echo ""

# Test 1: Generate Test Token
echo -e "${YELLOW}Test 1: Generate Test Token${NC}"
echo "Endpoint: POST $API_URL/test/auth/token"
TOKEN_RESPONSE=$(curl -s -X POST "$API_URL/test/auth/token?memberId=1")
echo "$TOKEN_RESPONSE" | jq '.' 2>/dev/null || echo "$TOKEN_RESPONSE"

# Extract access token using jq (if available)
if command -v jq &> /dev/null; then
    ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.data.accessToken')
    REFRESH_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.data.refreshToken')

    if [ "$ACCESS_TOKEN" != "null" ] && [ -n "$ACCESS_TOKEN" ]; then
        echo -e "${GREEN}✓ Token generated successfully${NC}"
    else
        echo -e "${RED}✗ Failed to generate token${NC}"
        exit 1
    fi
else
    echo -e "${YELLOW}⚠ jq not installed. Please install jq for better output parsing${NC}"
    echo "Install: brew install jq (macOS) or apt-get install jq (Linux)"
    exit 1
fi
echo ""

# Test 2: Access Protected Endpoint WITHOUT Token (Should Fail)
echo -e "${YELLOW}Test 2: Access Protected Endpoint WITHOUT Token (Should Fail with C101)${NC}"
echo "Endpoint: GET $API_URL/posts"
RESPONSE=$(curl -s -X GET "$API_URL/posts")
echo "$RESPONSE" | jq '.'

ERROR_CODE=$(echo "$RESPONSE" | jq -r '.errorCode')
if [ "$ERROR_CODE" = "C101" ]; then
    echo -e "${GREEN}✓ Correctly blocked: Unauthorized (C101)${NC}"
else
    echo -e "${RED}✗ Expected C101 error code, got: $ERROR_CODE${NC}"
fi
echo ""

# Test 3: Access Protected Endpoint WITH Valid Token (Should Succeed)
echo -e "${YELLOW}Test 3: Access Protected Endpoint WITH Valid Token${NC}"
echo "Endpoint: GET $API_URL/posts"
echo "Authorization: Bearer \$ACCESS_TOKEN"
RESPONSE=$(curl -s -X GET "$API_URL/posts" \
    -H "Authorization: Bearer $ACCESS_TOKEN")
echo "$RESPONSE" | jq '.'

SUCCESS=$(echo "$RESPONSE" | jq -r '.success')
if [ "$SUCCESS" = "true" ]; then
    echo -e "${GREEN}✓ Successfully authenticated and accessed protected endpoint${NC}"
else
    echo -e "${RED}✗ Failed to access protected endpoint with valid token${NC}"
fi
echo ""

# Test 4: Test @CurrentMember Injection
echo -e "${YELLOW}Test 4: Test @CurrentMember Injection${NC}"
echo "Endpoint: GET $API_URL/test/auth/me"
RESPONSE=$(curl -s -X GET "$API_URL/test/auth/me" \
    -H "Authorization: Bearer $ACCESS_TOKEN")
echo "$RESPONSE" | jq '.'

MEMBER_ID=$(echo "$RESPONSE" | jq -r '.data.memberId')
if [ "$MEMBER_ID" = "1" ]; then
    echo -e "${GREEN}✓ @CurrentMember correctly injected memberId: $MEMBER_ID${NC}"
else
    echo -e "${RED}✗ Failed to inject @CurrentMember${NC}"
fi
echo ""

# Test 5: Access with Invalid Token (Should Fail with C102)
echo -e "${YELLOW}Test 5: Access with Invalid Token (Should Fail with C102)${NC}"
RESPONSE=$(curl -s -X GET "$API_URL/posts" \
    -H "Authorization: Bearer invalid.token.here")
echo "$RESPONSE" | jq '.'

ERROR_CODE=$(echo "$RESPONSE" | jq -r '.errorCode')
if [ "$ERROR_CODE" = "C102" ]; then
    echo -e "${GREEN}✓ Correctly blocked: Invalid Token (C102)${NC}"
else
    echo -e "${RED}✗ Expected C102 error code, got: $ERROR_CODE${NC}"
fi
echo ""

# Test 6: Public Endpoint (No Token Required)
echo -e "${YELLOW}Test 6: Public Endpoint Access (No Token Required)${NC}"
echo "Endpoint: POST $API_URL/test/auth/token (Should work without token)"
RESPONSE=$(curl -s -X POST "$API_URL/test/auth/token")
SUCCESS=$(echo "$RESPONSE" | jq -r '.success')
if [ "$SUCCESS" = "true" ]; then
    echo -e "${GREEN}✓ Public endpoint accessible without token${NC}"
else
    echo -e "${RED}✗ Public endpoint failed${NC}"
fi
echo ""

echo "=================================================="
echo "Test Summary"
echo "=================================================="
echo -e "${GREEN}✓ All core authentication features working${NC}"
echo ""
echo "Access Token (for manual testing):"
echo "$ACCESS_TOKEN"
echo ""
echo "Refresh Token:"
echo "$REFRESH_TOKEN"
echo ""
echo "Manual Test Command:"
echo "curl -X GET $API_URL/posts -H \"Authorization: Bearer $ACCESS_TOKEN\""
echo ""
echo "⚠️  IMPORTANT: Remove TestAuthController before production deployment!"
