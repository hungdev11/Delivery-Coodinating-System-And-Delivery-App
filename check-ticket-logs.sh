#!/bin/bash

echo "=== Checking Ticket Creation Logs ==="
echo ""

echo "1. SESSION-SERVICE - DELIVERY_FAILED ticket logs:"
echo "----------------------------------------"
docker logs dss-session-service 2>&1 | grep -i "ticket" | tail -20
echo ""

echo "2. SESSION-SERVICE - Attempting to create DELIVERY_FAILED:"
echo "----------------------------------------"
docker logs dss-session-service 2>&1 | grep -i "Attempting to create DELIVERY_FAILED ticket" | tail -10
echo ""

echo "3. SESSION-SERVICE - Failed/Success messages:"
echo "----------------------------------------"
docker logs dss-session-service 2>&1 | grep -E "Successfully created DELIVERY_FAILED|Failed to create DELIVERY_FAILED" | tail -10
echo ""

echo "4. PARCEL-SERVICE - NOT_RECEIVED ticket logs:"
echo "----------------------------------------"
docker logs dss-parcel-service 2>&1 | grep -i "ticket" | tail -20
echo ""

echo "5. COMMUNICATION-SERVICE - Ticket creation logs:"
echo "----------------------------------------"
docker logs dss-communication-service 2>&1 | grep -i "Creating.*ticket" | tail -10
echo ""

echo "6. Connection errors (Feign/Connection refused):"
echo "----------------------------------------"
docker logs dss-session-service 2>&1 | grep -iE "feign|connection.*refused|connectexception" | tail -10
docker logs dss-parcel-service 2>&1 | grep -iE "feign|connection.*refused|connectexception" | tail -10
echo ""

echo "7. Recent errors from all services:"
echo "----------------------------------------"
docker logs dss-session-service 2>&1 | grep -i "error\|exception" | tail -5
docker logs dss-parcel-service 2>&1 | grep -i "error\|exception" | tail -5
docker logs dss-communication-service 2>&1 | grep -i "error\|exception" | tail -5
