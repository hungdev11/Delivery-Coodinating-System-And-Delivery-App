/**
 * Prisma Client Singleton
 * Ensures only one instance of PrismaClient exists throughout the application
 */

import { PrismaClient } from '@prisma/client';

class PrismaClientSingleton {
  private static instance: PrismaClient | null = null;

  private constructor() {}

  /**
   * Get the singleton instance of PrismaClient
   */
  public static getInstance(): PrismaClient {
    if (!PrismaClientSingleton.instance) {
      PrismaClientSingleton.instance = new PrismaClient({
        log: process.env.NODE_ENV === 'development' 
          ? ['query', 'info', 'warn', 'error'] 
          : ['warn', 'error'],
      });

      // Handle graceful shutdown
      const cleanup = async () => {
        if (PrismaClientSingleton.instance) {
          await PrismaClientSingleton.instance.$disconnect();
          PrismaClientSingleton.instance = null;
        }
      };

      process.on('beforeExit', cleanup);
      process.on('SIGINT', cleanup);
      process.on('SIGTERM', cleanup);
    }

    return PrismaClientSingleton.instance;
  }

  /**
   * Disconnect from the database
   */
  public static async disconnect(): Promise<void> {
    if (PrismaClientSingleton.instance) {
      await PrismaClientSingleton.instance.$disconnect();
      PrismaClientSingleton.instance = null;
    }
  }

  /**
   * Check if client is connected
   */
  public static isConnected(): boolean {
    return PrismaClientSingleton.instance !== null;
  }
}

// Export the singleton instance
export const prisma = PrismaClientSingleton.getInstance();

// Export the class for advanced use cases
export { PrismaClientSingleton };
