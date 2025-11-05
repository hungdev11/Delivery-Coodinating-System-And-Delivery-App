/**
 * Flow Check System (Optional)
 * 
 * Comprehensive validation system that runs all check scripts.
 * These checks are OPTIONAL - they only report statistics and don't fail the workflow.
 * 
 * Usage: npm run check:all
 */

import { spawn } from 'child_process';
import { join } from 'path';

interface CheckResult {
  name: string;
  success: boolean;
  duration: number;
  error?: string;
}

const CHECK_SCRIPTS = [
  {
    name: 'Road Connectivity Check',
    script: 'processors/check/check-road-connectivity.ts',
    description: 'Analyzes road network connectivity and finds connected components',
  },
  {
    name: 'Node Sharing Check',
    script: 'processors/check/check-node-sharing.ts',
    description: 'Checks if roads share node IDs at intersection points',
  },
  {
    name: 'Road Segment Debug',
    script: 'processors/check/debug-segments.ts',
    description: 'Debugs road segments for data integrity',
  },
];

async function runCheck(scriptPath: string, name: string): Promise<CheckResult> {
  return new Promise((resolve) => {
    const startTime = Date.now();
    
    console.log(`\n${'='.repeat(70)}`);
    console.log(`🔍 Running ${name}...`);
    console.log('='.repeat(70));

    const child = spawn('tsx', [scriptPath], {
      stdio: 'inherit',
      shell: true,
      cwd: join(__dirname, '../..'),
    });

    let errorOutput = '';

    child.on('error', (error) => {
      errorOutput = error.message;
      console.error(`⚠️  Error running ${name}:`, error);
    });

    child.on('close', (code) => {
      const duration = Date.now() - startTime;
      // Always consider as success for optional checks (only report statistics)
      const success = true;

      if (code === 0) {
        console.log(`✅ ${name} completed successfully (${duration}ms)`);
      } else {
        console.log(`⚠️  ${name} completed with warnings (${duration}ms)`);
        console.log(`   Note: This is an optional check for statistics only`);
      }

      resolve({
        name,
        success,
        duration,
        error: errorOutput || (code !== 0 ? `Process exited with code ${code} (optional)` : undefined),
      });
    });
  });
}

async function main() {
  console.log('\n' + '='.repeat(70));
  console.log('🚀 Starting Flow Check System (Optional - Statistics Only)');
  console.log('='.repeat(70));
  console.log(`\n📋 Running ${CHECK_SCRIPTS.length} optional checks:\n`);

  CHECK_SCRIPTS.forEach((check, index) => {
    console.log(`  ${index + 1}. ${check.name}`);
    console.log(`     ${check.description}`);
  });

  console.log('\n💡 Note: These checks are OPTIONAL and only report statistics.');
  console.log('   They will not fail the workflow even if issues are found.\n');

  const results: CheckResult[] = [];

  for (const check of CHECK_SCRIPTS) {
    const result = await runCheck(check.script, check.name);
    results.push(result);
  }

  // Summary
  console.log('\n' + '='.repeat(70));
  console.log('📊 Flow Check Summary (Optional)');
  console.log('='.repeat(70));

  const successful = results.filter(r => r.success && !r.error).length;
  const withWarnings = results.filter(r => r.error).length;
  const totalDuration = results.reduce((sum, r) => sum + r.duration, 0);

  console.log(`\n✅ Completed: ${successful}/${results.length}`);
  console.log(`⚠️  With Warnings: ${withWarnings}/${results.length}`);
  console.log(`⏱️  Total Duration: ${totalDuration}ms\n`);

  results.forEach((result) => {
    const status = result.error ? '⚠️' : '✅';
    console.log(`${status} ${result.name}: ${result.duration}ms`);
    if (result.error) {
      console.log(`   ${result.error}`);
    }
  });

  console.log('\n' + '='.repeat(70));
  console.log('✅ All checks completed (optional - workflow continues)');
  console.log('='.repeat(70));

  // Always exit with success (0) for optional checks
  process.exit(0);
}

// Run if executed directly
if (import.meta.url === `file://${process.argv[1]}`) {
  main().catch((error) => {
    console.error('\n⚠️  Flow check system encountered an error (optional):', error);
    // Even on error, exit with success for optional checks
    process.exit(0);
  });
}

export { main as runFlowCheck };
