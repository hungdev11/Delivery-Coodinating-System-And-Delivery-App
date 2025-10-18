#!/usr/bin/env node

/**
 * OSRM Manager Script
 * 
 * Command-line tool for managing OSRM instances
 * Usage: node scripts/osrm-manager.js <command> [options]
 */

const { exec } = require('child_process');
const { promisify } = require('util');
const fs = require('fs');
const path = require('path');

const execAsync = promisify(exec);

const OSRM_DATA_PATH = '/app/osrm_data';
const OSM_FILE_PATH = '/app/raw_data/vietnam/vietnam-251013.osm.pbf';

class OSRMManager {
  constructor() {
    this.instances = [
      { id: 1, name: 'osrm-instance-1', port: 5000 },
      { id: 2, name: 'osrm-instance-2', port: 5001 }
    ];
  }

  async buildInstance(instanceId) {
    console.log(`🔨 Building OSRM data for instance ${instanceId}...`);
    
    const instance = this.instances.find(i => i.id === instanceId);
    if (!instance) {
      throw new Error(`Invalid instance ID: ${instanceId}`);
    }

    const dataPath = path.join(OSRM_DATA_PATH, instance.name);
    
    // Ensure directory exists
    if (!fs.existsSync(dataPath)) {
      fs.mkdirSync(dataPath, { recursive: true });
    }

    try {
      // Extract
      console.log('  📦 Extracting OSM data...');
      const extractCmd = `osrm-extract -p ${dataPath}/custom_car.lua ${OSM_FILE_PATH}`;
      await execAsync(extractCmd, { cwd: dataPath });

      // Contract
      console.log('  🔗 Contracting OSRM data...');
      const contractCmd = `osrm-contract ${dataPath}/network.osrm`;
      await execAsync(contractCmd, { cwd: dataPath });

      console.log(`✅ OSRM data built successfully for instance ${instanceId}`);
      return true;
    } catch (error) {
      console.error(`❌ Failed to build OSRM data for instance ${instanceId}:`, error.message);
      return false;
    }
  }

  async buildAll() {
    console.log('🔨 Building OSRM data for all instances...');
    
    const results = [];
    for (const instance of this.instances) {
      const success = await this.buildInstance(instance.id);
      results.push({ instance: instance.id, success });
    }

    const successCount = results.filter(r => r.success).length;
    console.log(`📊 Build completed: ${successCount}/${this.instances.length} instances successful`);
    
    return results;
  }

  async startInstance(instanceId) {
    console.log(`🚀 Starting OSRM instance ${instanceId}...`);
    
    const instance = this.instances.find(i => i.id === instanceId);
    if (!instance) {
      throw new Error(`Invalid instance ID: ${instanceId}`);
    }

    const dataPath = path.join(OSRM_DATA_PATH, instance.name);
    const dataFile = path.join(dataPath, 'network.osrm');

    if (!fs.existsSync(dataFile)) {
      throw new Error(`OSRM data not found for instance ${instanceId}. Run build first.`);
    }

    try {
      const startCmd = `osrm-routed --algorithm mld --port ${instance.port} ${dataFile}`;
      const child = exec(startCmd, { cwd: dataPath });
      
      console.log(`✅ OSRM instance ${instanceId} started on port ${instance.port} (PID: ${child.pid})`);
      return child;
    } catch (error) {
      console.error(`❌ Failed to start OSRM instance ${instanceId}:`, error.message);
      return null;
    }
  }

  async stopInstance(instanceId) {
    console.log(`🛑 Stopping OSRM instance ${instanceId}...`);
    
    // Find process by port
    try {
      const { stdout } = await execAsync(`lsof -ti:${this.instances[instanceId - 1].port}`);
      const pid = stdout.trim();
      
      if (pid) {
        await execAsync(`kill -TERM ${pid}`);
        console.log(`✅ OSRM instance ${instanceId} stopped (PID: ${pid})`);
        return true;
      } else {
        console.log(`ℹ️  No OSRM instance ${instanceId} running`);
        return true;
      }
    } catch (error) {
      console.log(`ℹ️  No OSRM instance ${instanceId} running`);
      return true;
    }
  }

  async rollingRestart() {
    console.log('🔄 Starting rolling restart...');
    
    // Determine current and next instance
    const currentInstance = 1; // Assume instance 1 is currently active
    const nextInstance = currentInstance === 1 ? 2 : 1;
    
    console.log(`  Stopping instance ${currentInstance}...`);
    await this.stopInstance(currentInstance);
    
    console.log(`  Starting instance ${nextInstance}...`);
    await this.startInstance(nextInstance);
    
    console.log(`✅ Rolling restart completed. Active instance: ${nextInstance}`);
  }

  async status() {
    console.log('📊 OSRM Instances Status:');
    console.log('========================');
    
    for (const instance of this.instances) {
      try {
        const { stdout } = await execAsync(`lsof -ti:${instance.port}`);
        const pid = stdout.trim();
        
        if (pid) {
          console.log(`  Instance ${instance.id} (${instance.name}): ✅ Running (PID: ${pid}, Port: ${instance.port})`);
        } else {
          console.log(`  Instance ${instance.id} (${instance.name}): ❌ Stopped (Port: ${instance.port})`);
        }
      } catch (error) {
        console.log(`  Instance ${instance.id} (${instance.name}): ❌ Stopped (Port: ${instance.port})`);
      }
    }
  }

  async healthCheck() {
    console.log('🏥 OSRM Health Check:');
    console.log('====================');
    
    for (const instance of this.instances) {
      try {
        const response = await fetch(`http://localhost:${instance.port}/route/v1/driving/106.7718,10.8505;106.8032,10.8623?overview=false`);
        
        if (response.ok) {
          console.log(`  Instance ${instance.id}: ✅ Healthy`);
        } else {
          console.log(`  Instance ${instance.id}: ❌ Unhealthy (Status: ${response.status})`);
        }
      } catch (error) {
        console.log(`  Instance ${instance.id}: ❌ Unreachable`);
      }
    }
  }
}

// CLI Interface
async function main() {
  const command = process.argv[2];
  const manager = new OSRMManager();

  try {
    switch (command) {
      case 'build':
        const instanceId = parseInt(process.argv[3]);
        if (instanceId) {
          await manager.buildInstance(instanceId);
        } else {
          await manager.buildAll();
        }
        break;

      case 'start':
        const startInstanceId = parseInt(process.argv[3]);
        if (!startInstanceId) {
          console.error('❌ Instance ID required for start command');
          process.exit(1);
        }
        await manager.startInstance(startInstanceId);
        break;

      case 'stop':
        const stopInstanceId = parseInt(process.argv[3]);
        if (!stopInstanceId) {
          console.error('❌ Instance ID required for stop command');
          process.exit(1);
        }
        await manager.stopInstance(stopInstanceId);
        break;

      case 'restart':
        await manager.rollingRestart();
        break;

      case 'status':
        await manager.status();
        break;

      case 'health':
        await manager.healthCheck();
        break;

      default:
        console.log(`
OSRM Manager - Command Line Tool

Usage: node scripts/osrm-manager.js <command> [options]

Commands:
  build [instanceId]     Build OSRM data (for specific instance or all)
  start <instanceId>     Start OSRM instance
  stop <instanceId>      Stop OSRM instance
  restart                Rolling restart (stop current, start next)
  status                 Show instances status
  health                 Check instances health

Examples:
  node scripts/osrm-manager.js build          # Build all instances
  node scripts/osrm-manager.js build 1        # Build instance 1
  node scripts/osrm-manager.js start 1        # Start instance 1
  node scripts/osrm-manager.js restart        # Rolling restart
  node scripts/osrm-manager.js status         # Show status
        `);
    }
  } catch (error) {
    console.error('❌ Error:', error.message);
    process.exit(1);
  }
}

if (require.main === module) {
  main();
}

module.exports = OSRMManager;
