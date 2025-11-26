# System Diagrams

This folder contains sequence diagrams and state diagrams that illustrate system workflows and entity lifecycles. Diagrams are organized by functional area for easy reference.

## Table of Contents

- [Diagram Documents](#diagram-documents)
- [Usage](#usage)
- [Related Documentation](#related-documentation)

## Diagram Documents

### [Parcel Workflows](parcel_workflows.md)

Complete workflows for parcel operations including:
- Parcel state lifecycle
- Create parcel
- Delivery man accepts parcel
- Deliver parcel
- Confirm parcel
- Refuse parcel
- Dispute parcel
- Parcel and session state interaction

### [Session Workflows](session_workflows.md)

Complete workflows for delivery session operations including:
- Session state lifecycle
- Prepare session
- Start session
- Complete session
- Fail session

### [Routing Workflows](routing_workflows.md)

Complete workflows for routing and geographic operations including:
- Calculate route
- Get OSRM status
- Detailed routing creation

### [System Architecture Flows](system_flows.md)

System-level operations including:
- User login and authentication
- High-level layer data flow

## Usage

These diagrams are referenced in feature documentation to provide visual context for system workflows. They help understand the sequence of operations and state transitions that occur during various system processes.

Each diagram document groups related workflows together, making it easier to understand complete operational flows within each functional area.

## Related Documentation

- [API Documentation](../README.md) - Complete API endpoint documentation
- [Backend Services](../../2_BACKEND/) - Service architecture documentation
- [Features Documentation](../../features/README.md) - Feature workflows with diagram references
- [System Analysis](../../SYSTEM_ANALYSIS.md) - System architecture and technical assessment
