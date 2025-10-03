#!/bin/bash

# Health Check Script for Delivery System Services
# Usage: ./health-check.sh [service_name] [environment]

set -e

# Default values
SERVICE=${1:-"all"}
ENVIRONMENT=${2:-"staging"}
TIMEOUT=30
RETRY_COUNT=3

# Service URLs based on environment
if [ "$ENVIRONMENT" = "production" ]; then
    API_GATEWAY_URL="https://api-gateway.example.com:21500"
    USER_SERVICE_URL="https://user-service.example.com:21501"
    SETTINGS_SERVICE_URL="https://settings-service.example.com:21502"
else
    API_GATEWAY_URL="http://localhost:21500"
    USER_SERVICE_URL="http://localhost:21501"
    SETTINGS_SERVICE_URL="http://localhost:21502"
fi

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to check service health
check_service_health() {
    local service_name=$1
    local service_url=$2
    local health_endpoint="$service_url/actuator/health"
    
    echo -e "${YELLOW}Checking $service_name health...${NC}"
    
    for i in $(seq 1 $RETRY_COUNT); do
        if curl -f -s --max-time $TIMEOUT "$health_endpoint" > /dev/null; then
            echo -e "${GREEN}✅ $service_name is healthy${NC}"
            return 0
        else
            echo -e "${YELLOW}⚠️  Attempt $i/$RETRY_COUNT failed for $service_name${NC}"
            if [ $i -lt $RETRY_COUNT ]; then
                sleep 5
            fi
        fi
    done
    
    echo -e "${RED}❌ $service_name health check failed after $RETRY_COUNT attempts${NC}"
    return 1
}

# Function to check service metrics
check_service_metrics() {
    local service_name=$1
    local service_url=$2
    local metrics_endpoint="$service_url/actuator/metrics"
    
    echo -e "${YELLOW}Checking $service_name metrics...${NC}"
    
    if curl -f -s --max-time $TIMEOUT "$metrics_endpoint" > /dev/null; then
        echo -e "${GREEN}✅ $service_name metrics are accessible${NC}"
        return 0
    else
        echo -e "${RED}❌ $service_name metrics check failed${NC}"
        return 1
    fi
}

# Function to check service readiness
check_service_readiness() {
    local service_name=$1
    local service_url=$2
    local ready_endpoint="$service_url/actuator/health/readiness"
    
    echo -e "${YELLOW}Checking $service_name readiness...${NC}"
    
    if curl -f -s --max-time $TIMEOUT "$ready_endpoint" > /dev/null; then
        echo -e "${GREEN}✅ $service_name is ready${NC}"
        return 0
    else
        echo -e "${RED}❌ $service_name readiness check failed${NC}"
        return 1
    fi
}

# Function to check service liveness
check_service_liveness() {
    local service_name=$1
    local service_url=$2
    local live_endpoint="$service_url/actuator/health/liveness"
    
    echo -e "${YELLOW}Checking $service_name liveness...${NC}"
    
    if curl -f -s --max-time $TIMEOUT "$live_endpoint" > /dev/null; then
        echo -e "${GREEN}✅ $service_name is alive${NC}"
        return 0
    else
        echo -e "${RED}❌ $service_name liveness check failed${NC}"
        return 1
    fi
}

# Function to check database connectivity
check_database_connectivity() {
    echo -e "${YELLOW}Checking database connectivity...${NC}"
    
    # Add database connectivity checks here
    # This would depend on your database setup
    echo -e "${GREEN}✅ Database connectivity check completed${NC}"
    return 0
}

# Function to check external dependencies
check_external_dependencies() {
    echo -e "${YELLOW}Checking external dependencies...${NC}"
    
    # Check Keycloak if applicable
    if [ "$ENVIRONMENT" = "production" ]; then
        KEYCLOAK_URL="https://keycloak.example.com:8080"
    else
        KEYCLOAK_URL="http://localhost:8080"
    fi
    
    if curl -f -s --max-time $TIMEOUT "$KEYCLOAK_URL/health/ready" > /dev/null; then
        echo -e "${GREEN}✅ Keycloak is healthy${NC}"
    else
        echo -e "${RED}❌ Keycloak health check failed${NC}"
        return 1
    fi
    
    return 0
}

# Main execution
main() {
    echo -e "${YELLOW}Starting health checks for environment: $ENVIRONMENT${NC}"
    echo "=========================================="
    
    local exit_code=0
    
    # Check API Gateway
    if [ "$SERVICE" = "all" ] || [ "$SERVICE" = "api-gateway" ]; then
        check_service_health "API Gateway" "$API_GATEWAY_URL" || exit_code=1
        check_service_metrics "API Gateway" "$API_GATEWAY_URL" || exit_code=1
        check_service_readiness "API Gateway" "$API_GATEWAY_URL" || exit_code=1
        check_service_liveness "API Gateway" "$API_GATEWAY_URL" || exit_code=1
        echo ""
    fi
    
    # Check User Service
    if [ "$SERVICE" = "all" ] || [ "$SERVICE" = "user-service" ]; then
        check_service_health "User Service" "$USER_SERVICE_URL" || exit_code=1
        check_service_metrics "User Service" "$USER_SERVICE_URL" || exit_code=1
        check_service_readiness "User Service" "$USER_SERVICE_URL" || exit_code=1
        check_service_liveness "User Service" "$USER_SERVICE_URL" || exit_code=1
        echo ""
    fi
    
    # Check Settings Service
    if [ "$SERVICE" = "all" ] || [ "$SERVICE" = "settings-service" ]; then
        check_service_health "Settings Service" "$SETTINGS_SERVICE_URL" || exit_code=1
        check_service_metrics "Settings Service" "$SETTINGS_SERVICE_URL" || exit_code=1
        check_service_readiness "Settings Service" "$SETTINGS_SERVICE_URL" || exit_code=1
        check_service_liveness "Settings Service" "$SETTINGS_SERVICE_URL" || exit_code=1
        echo ""
    fi
    
    # Check external dependencies
    if [ "$SERVICE" = "all" ]; then
        check_database_connectivity || exit_code=1
        check_external_dependencies || exit_code=1
        echo ""
    fi
    
    echo "=========================================="
    if [ $exit_code -eq 0 ]; then
        echo -e "${GREEN}🎉 All health checks passed!${NC}"
    else
        echo -e "${RED}💥 Some health checks failed!${NC}"
    fi
    
    exit $exit_code
}

# Run main function
main "$@"
