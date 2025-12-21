/**
 * Audit Event Publisher
 * Publishes audit events to Kafka for all CRUD operations
 */

import { kafkaService } from './kafka.service';
import { logger } from '../logger/logger.service';
import { randomUUID } from 'crypto';

const TOPIC_AUDIT_EVENTS = 'audit-events';
const TOPIC_AUDIT_EVENTS_DLQ = 'audit-events-dlq';

export enum OperationType {
  CREATE = 'CREATE',
  UPDATE = 'UPDATE',
  DELETE = 'DELETE',
  READ = 'READ',
  CONNECT = 'CONNECT',
  DISCONNECT = 'DISCONNECT',
  MESSAGE = 'MESSAGE',
}

export enum Status {
  SUCCESS = 'SUCCESS',
  FAILED = 'FAILED',
  PENDING = 'PENDING',
}

export interface AuditEventDto {
  eventId?: string;
  timestamp?: string; // ISO 8601 format
  operationType: OperationType;
  sourceService?: string;
  userId?: string;
  userRoles?: string;
  httpMethod?: string;
  endpoint?: string;
  resourceType?: string;
  resourceId?: string;
  requestPayload?: Record<string, any>;
  responseStatus?: number;
  status?: Status;
  errorMessage?: string;
  errorStackTrace?: string;
  requestId?: string;
  metadata?: Record<string, any>;
  durationMs?: number;
  clientIp?: string;
  userAgent?: string;
}

class AuditEventPublisher {
  private static instance: AuditEventPublisher | null = null;

  private constructor() {}

  /**
   * Get singleton instance
   */
  public static getInstance(): AuditEventPublisher {
    if (!AuditEventPublisher.instance) {
      AuditEventPublisher.instance = new AuditEventPublisher();
    }
    return AuditEventPublisher.instance;
  }

  /**
   * Publish audit event to Kafka
   * Uses async send with error handling
   */
  public async publishAuditEvent(event: AuditEventDto): Promise<void> {
    try {
      // Generate event ID if not provided
      if (!event.eventId) {
        event.eventId = randomUUID();
      }

      // Set timestamp if not provided
      if (!event.timestamp) {
        event.timestamp = new Date().toISOString();
      }

      // Set source service
      if (!event.sourceService) {
        event.sourceService = 'zone-service';
      }

      // Use resourceId or userId as key for partitioning
      const key = event.resourceId || event.userId || event.eventId || 'unknown';

      // Skip if Kafka is not connected
      if (!kafkaService.getConnectionStatus()) {
        logger.warn('[AuditEventPublisher] Kafka not connected, audit event will be skipped', {
          eventId: event.eventId,
          operation: event.operationType,
        });
        return;
      }

      // Send to Kafka asynchronously (non-blocking)
      await kafkaService.sendMessage(TOPIC_AUDIT_EVENTS, event, key);

      logger.debug('[AuditEventPublisher] Audit event published successfully', {
        eventId: event.eventId,
        operation: event.operationType,
        resourceType: event.resourceType,
      });
    } catch (error: any) {
      logger.error('[AuditEventPublisher] Failed to publish audit event', {
        eventId: event.eventId,
        operation: event.operationType,
        error: error.message,
      });

      // Send to Dead Letter Queue for manual review
      await this.sendToDeadLetterQueue(event, error);
    }
  }

  /**
   * Send failed event to Dead Letter Queue
   */
  private async sendToDeadLetterQueue(event: AuditEventDto, error: Error): Promise<void> {
    try {
      // Mark as failed
      event.status = Status.FAILED;
      event.errorMessage = error.message;

      // Truncate stack trace if too long (max 5000 chars)
      if (error.stack) {
        const stackTrace = error.stack.length > 5000
          ? error.stack.substring(0, 5000) + '... (truncated)'
          : error.stack;
        event.errorStackTrace = stackTrace;
      }

      // Skip if Kafka is not connected
      if (!kafkaService.getConnectionStatus()) {
        logger.error('[AuditEventPublisher] Kafka not connected, cannot send to DLQ', {
          eventId: event.eventId,
        });
        return;
      }

      // Send to DLQ
      await kafkaService.sendMessage(TOPIC_AUDIT_EVENTS_DLQ, event, event.eventId || 'unknown');
      logger.debug('[AuditEventPublisher] Audit event sent to DLQ', {
        eventId: event.eventId,
      });
    } catch (dlqError: any) {
      logger.error('[AuditEventPublisher] CRITICAL: Failed to send audit event to DLQ', {
        eventId: event.eventId,
        error: dlqError.message,
      });
      // At this point, we can only log to application logs
    }
  }

  /**
   * Helper method to create audit event from operation details
   */
  public async logOperation(
    operationType: OperationType,
    httpMethod?: string,
    endpoint?: string,
    resourceType?: string,
    resourceId?: string,
    userId?: string,
    userRoles?: string,
    responseStatus?: number,
    status: Status = Status.SUCCESS,
    durationMs?: number,
    requestId?: string,
    clientIp?: string,
    userAgent?: string,
    requestPayload?: Record<string, any>,
    errorMessage?: string
  ): Promise<void> {
    const event: AuditEventDto = {
      operationType,
      status,
      ...(httpMethod !== undefined && { httpMethod }),
      ...(endpoint !== undefined && { endpoint }),
      ...(resourceType !== undefined && { resourceType }),
      ...(resourceId !== undefined && { resourceId }),
      ...(userId !== undefined && { userId }),
      ...(userRoles !== undefined && { userRoles }),
      ...(responseStatus !== undefined && { responseStatus }),
      ...(durationMs !== undefined && { durationMs }),
      ...(requestId !== undefined && { requestId }),
      ...(clientIp !== undefined && { clientIp }),
      ...(userAgent !== undefined && { userAgent }),
      ...(requestPayload !== undefined && { requestPayload }),
      ...(errorMessage !== undefined && { errorMessage }),
    };

    await this.publishAuditEvent(event);
  }
}

// Export singleton instance
export const auditEventPublisher = AuditEventPublisher.getInstance();

// Export class for advanced use cases
export { AuditEventPublisher };
