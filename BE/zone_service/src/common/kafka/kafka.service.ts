/**
 * Kafka Service
 * Manages Kafka producer and consumer as background jobs
 */

import { Kafka, Producer, Consumer, logLevel } from 'kafkajs';
import { logger } from '../logger/logger.service';

export interface KafkaConfig {
  brokers: string[];
  clientId: string;
  groupId?: string;
}

class KafkaService {
  private static instance: KafkaService | null = null;
  private kafka: Kafka | null = null;
  private producer: Producer | null = null;
  private consumer: Consumer | null = null;
  private isConnected: boolean = false;

  private constructor() {}

  /**
   * Get the singleton instance of KafkaService
   */
  public static getInstance(): KafkaService {
    if (!KafkaService.instance) {
      KafkaService.instance = new KafkaService();
    }
    return KafkaService.instance;
  }

  /**
   * Initialize Kafka connection
   */
  public async initialize(config: KafkaConfig): Promise<void> {
    if (this.isConnected) {
      logger.warn('Kafka is already connected');
      return;
    }

    try {
      this.kafka = new Kafka({
        clientId: config.clientId,
        brokers: config.brokers,
        logLevel: logLevel.ERROR,
      });

      this.producer = this.kafka.producer();
      await this.producer.connect();

      if (config.groupId) {
        this.consumer = this.kafka.consumer({ groupId: config.groupId });
        await this.consumer.connect();
      }

      this.isConnected = true;
      logger.info('Kafka connected successfully', { brokers: config.brokers });
    } catch (error) {
      logger.error('Failed to connect to Kafka', { error });
      throw error;
    }
  }

  /**
   * Send a message to a Kafka topic (non-blocking)
   */
  public async sendMessage(topic: string, message: any): Promise<void> {
    if (!this.producer || !this.isConnected) {
      logger.warn('Kafka producer is not connected, message will be skipped', { topic, message });
      return;
    }

    // Run in background without blocking
    setImmediate(async () => {
      try {
        await this.producer!.send({
          topic,
          messages: [
            {
              value: JSON.stringify(message),
            },
          ],
        });
        logger.debug('Message sent to Kafka', { topic });
      } catch (error) {
        logger.error('Failed to send message to Kafka', { topic, error });
      }
    });
  }

  /**
   * Subscribe to a topic and process messages
   */
  public async subscribe(topic: string, handler: (message: any) => Promise<void>): Promise<void> {
    if (!this.consumer || !this.isConnected) {
      logger.warn('Kafka consumer is not connected');
      return;
    }

    await this.consumer.subscribe({ topic, fromBeginning: false });

    // Run consumer in background
    setImmediate(async () => {
      try {
        await this.consumer!.run({
          eachMessage: async ({ topic, partition, message }) => {
            try {
              const data = JSON.parse(message.value?.toString() || '{}');
              await handler(data);
              logger.debug('Message processed from Kafka', { topic, partition });
            } catch (error) {
              logger.error('Failed to process Kafka message', { topic, partition, error });
            }
          },
        });
      } catch (error) {
        logger.error('Kafka consumer error', { error });
      }
    });
  }

  /**
   * Disconnect from Kafka
   */
  public async disconnect(): Promise<void> {
    if (this.producer) {
      await this.producer.disconnect();
      this.producer = null;
    }

    if (this.consumer) {
      await this.consumer.disconnect();
      this.consumer = null;
    }

    this.isConnected = false;
    this.kafka = null;
    logger.info('Kafka disconnected');
  }

  /**
   * Check if Kafka is connected
   */
  public getConnectionStatus(): boolean {
    return this.isConnected;
  }
}

// Export the singleton instance
export const kafkaService = KafkaService.getInstance();

// Export the class for advanced use cases
export { KafkaService };
