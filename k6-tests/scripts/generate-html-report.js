#!/usr/bin/env node

const fs = require('fs');
const path = require('path');
const Handlebars = require('handlebars');

/**
 * K6 HTML Report Generator
 * Converts K6 JSON results into a beautiful HTML report
 */

// Helper functions
const formatDuration = (ms) => {
  if (ms < 1000) return `${ms.toFixed(2)}ms`;
  if (ms < 60000) return `${(ms / 1000).toFixed(2)}s`;
  return `${(ms / 60000).toFixed(2)}m`;
};

const formatNumber = (num) => {
  return new Intl.NumberFormat('en-US').format(num);
};

const getStatusColor = (value, threshold) => {
  if (value < threshold * 0.5) return 'success';
  if (value < threshold) return 'warning';
  return 'danger';
};

// Register Handlebars helpers
Handlebars.registerHelper('formatDuration', formatDuration);
Handlebars.registerHelper('formatNumber', formatNumber);
Handlebars.registerHelper('percentage', (value) => `${(value * 100).toFixed(2)}%`);
Handlebars.registerHelper('json', (obj) => JSON.stringify(obj, null, 2));

/**
 * Parse K6 JSON results with streaming for large files
 */
function parseK6Results(jsonFile) {
  const readline = require('readline');
  const stream = fs.createReadStream(jsonFile);
  
  return new Promise((resolve, reject) => {
    const rl = readline.createInterface({
      input: stream,
      crlfDelay: Infinity
    });

    const metrics = {};
    const points = [];
    let testInfo = {};
    let lineCount = 0;
    const MAX_POINTS = 10000; // Limit data points to prevent memory issues

    rl.on('line', (line) => {
      try {
        lineCount++;
        const data = JSON.parse(line);
        
        if (data.type === 'Metric') {
          metrics[data.metric] = data.data;
        } else if (data.type === 'Point') {
          // Sample points to avoid memory issues
          if (points.length < MAX_POINTS) {
            points.push(data);
          } else if (lineCount % 10 === 0) {
            // Sample 10% after limit reached
            points.push(data);
          }
        } else if (data.metric && data.data) {
          metrics[data.metric] = data.data;
        }
      } catch (e) {
        // Skip invalid lines
      }
    });

    rl.on('close', () => {
      console.log(`Processed ${lineCount} lines, collected ${points.length} data points`);
      resolve({ metrics, points, testInfo });
    });

    rl.on('error', (error) => {
      reject(error);
    });
  });
}

/**
 * Aggregate metrics from K6 data points
 */
function aggregateMetrics(points) {
  const metricData = {};

  points.forEach(point => {
    const metricName = point.metric;
    if (!metricData[metricName]) {
      metricData[metricName] = {
        values: [],
        tags: point.data?.tags || {}
      };
    }
    if (point.data?.value !== undefined) {
      metricData[metricName].values.push(point.data.value);
    }
  });

  const aggregated = {};
  Object.keys(metricData).forEach(metric => {
    const values = metricData[metric].values;
    if (values.length === 0) return;

    values.sort((a, b) => a - b);
    const sum = values.reduce((a, b) => a + b, 0);
    const avg = sum / values.length;
    const min = values[0];
    const max = values[values.length - 1];
    const p50 = values[Math.floor(values.length * 0.5)];
    const p95 = values[Math.floor(values.length * 0.95)];
    const p99 = values[Math.floor(values.length * 0.99)];

    aggregated[metric] = {
      count: values.length,
      min,
      max,
      avg,
      p50,
      p95,
      p99,
      tags: metricData[metric].tags
    };
  });

  return aggregated;
}

/**
 * Generate HTML report
 */
async function generateReport(resultsDir, outputFile) {
  console.log(`📊 Generating HTML report from: ${resultsDir}`);

  // Find all JSON result files
  const files = fs.readdirSync(resultsDir)
    .filter(f => f.endsWith('.json'))
    .map(f => path.join(resultsDir, f));

  if (files.length === 0) {
    console.error('❌ No JSON result files found in:', resultsDir);
    process.exit(1);
  }

  console.log(`📄 Found ${files.length} test result file(s)`);

  // Parse all test results (streaming for large files)
  const testResults = [];
  for (const file of files) {
    const testName = path.basename(file, '.json');
    console.log(`   - Processing: ${testName}`);
    
    try {
      const { metrics, points } = await parseK6Results(file);
      const aggregated = aggregateMetrics(points);

      testResults.push({
        name: testName,
        file: path.basename(file),
        metrics: aggregated,
        rawMetrics: metrics
      });
    } catch (error) {
      console.error(`   ⚠️  Error processing ${testName}:`, error.message);
      testResults.push({
        name: testName,
        file: path.basename(file),
        error: error.message
      });
    }
  }

  // Calculate summary statistics
  const summary = {
    totalTests: testResults.length,
    timestamp: new Date().toISOString(),
    resultsDir: path.basename(resultsDir)
  };

  // HTML template
  const template = `
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>K6 Test Results - Meeting Room Booking</title>
  <style>
    * {
      margin: 0;
      padding: 0;
      box-sizing: border-box;
    }

    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      min-height: 100vh;
      padding: 20px;
    }

    .container {
      max-width: 1400px;
      margin: 0 auto;
    }

    .header {
      background: white;
      border-radius: 12px;
      padding: 30px;
      margin-bottom: 20px;
      box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
    }

    .header h1 {
      color: #333;
      font-size: 32px;
      margin-bottom: 10px;
    }

    .header .subtitle {
      color: #666;
      font-size: 16px;
    }

    .summary {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: 20px;
      margin-bottom: 20px;
    }

    .summary-card {
      background: white;
      border-radius: 12px;
      padding: 25px;
      box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
    }

    .summary-card .label {
      color: #666;
      font-size: 14px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      margin-bottom: 10px;
    }

    .summary-card .value {
      color: #333;
      font-size: 36px;
      font-weight: bold;
    }

    .test-section {
      background: white;
      border-radius: 12px;
      padding: 30px;
      margin-bottom: 20px;
      box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
    }

    .test-section h2 {
      color: #333;
      font-size: 24px;
      margin-bottom: 20px;
      padding-bottom: 10px;
      border-bottom: 2px solid #667eea;
    }

    .metrics-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
      gap: 15px;
      margin-bottom: 20px;
    }

    .metric-card {
      background: #f8f9fa;
      border-radius: 8px;
      padding: 20px;
      border-left: 4px solid #667eea;
    }

    .metric-card .metric-name {
      color: #666;
      font-size: 14px;
      margin-bottom: 10px;
      font-weight: 500;
    }

    .metric-stats {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 10px;
      font-size: 13px;
    }

    .metric-stats .stat {
      display: flex;
      justify-content: space-between;
    }

    .metric-stats .stat-label {
      color: #666;
    }

    .metric-stats .stat-value {
      color: #333;
      font-weight: 600;
    }

    .badge {
      display: inline-block;
      padding: 4px 12px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 600;
      text-transform: uppercase;
    }

    .badge.success {
      background: #d4edda;
      color: #155724;
    }

    .badge.warning {
      background: #fff3cd;
      color: #856404;
    }

    .badge.danger {
      background: #f8d7da;
      color: #721c24;
    }

    .badge.info {
      background: #d1ecf1;
      color: #0c5460;
    }

    .footer {
      text-align: center;
      color: white;
      padding: 20px;
      font-size: 14px;
    }

    table {
      width: 100%;
      border-collapse: collapse;
      margin-top: 15px;
    }

    th {
      background: #667eea;
      color: white;
      padding: 12px;
      text-align: left;
      font-size: 13px;
      font-weight: 600;
    }

    td {
      padding: 12px;
      border-bottom: 1px solid #eee;
      font-size: 13px;
    }

    tr:hover {
      background: #f8f9fa;
    }

    .progress-bar {
      width: 100%;
      height: 8px;
      background: #e9ecef;
      border-radius: 4px;
      overflow: hidden;
      margin-top: 10px;
    }

    .progress-fill {
      height: 100%;
      background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
      transition: width 0.3s ease;
    }

    @media print {
      body {
        background: white;
      }
      .test-section, .header, .summary-card {
        box-shadow: none;
        border: 1px solid #ddd;
      }
    }
  </style>
</head>
<body>
  <div class="container">
    <!-- Header -->
    <div class="header">
      <h1>🚀 K6 Performance Test Results</h1>
      <p class="subtitle">Meeting Room Booking System - Load Testing Report</p>
      <p class="subtitle" style="margin-top: 10px;">Generated: {{summary.timestamp}}</p>
    </div>

    <!-- Summary Cards -->
    <div class="summary">
      <div class="summary-card">
        <div class="label">Total Tests</div>
        <div class="value">{{summary.totalTests}}</div>
      </div>
      <div class="summary-card">
        <div class="label">Results Directory</div>
        <div class="value" style="font-size: 20px; word-break: break-all;">{{summary.resultsDir}}</div>
      </div>
    </div>

    <!-- Test Results -->
    {{#each tests}}
    <div class="test-section">
      <h2>📊 {{name}}</h2>
      <p style="color: #666; margin-bottom: 20px;">Test file: <code>{{file}}</code></p>

      {{#if metrics}}
      <div class="metrics-grid">
        {{#each metrics}}
        <div class="metric-card">
          <div class="metric-name">{{@key}}</div>
          <div class="metric-stats">
            <div class="stat">
              <span class="stat-label">Count:</span>
              <span class="stat-value">{{formatNumber count}}</span>
            </div>
            <div class="stat">
              <span class="stat-label">Avg:</span>
              <span class="stat-value">{{formatDuration avg}}</span>
            </div>
            <div class="stat">
              <span class="stat-label">Min:</span>
              <span class="stat-value">{{formatDuration min}}</span>
            </div>
            <div class="stat">
              <span class="stat-label">Max:</span>
              <span class="stat-value">{{formatDuration max}}</span>
            </div>
            <div class="stat">
              <span class="stat-label">P50:</span>
              <span class="stat-value">{{formatDuration p50}}</span>
            </div>
            <div class="stat">
              <span class="stat-label">P95:</span>
              <span class="stat-value">{{formatDuration p95}}</span>
            </div>
            <div class="stat">
              <span class="stat-label">P99:</span>
              <span class="stat-value">{{formatDuration p99}}</span>
            </div>
          </div>
        </div>
        {{/each}}
      </div>
      {{else}}
      <p style="color: #666;">No detailed metrics available for this test.</p>
      {{/if}}
    </div>
    {{/each}}

    <!-- Footer -->
    <div class="footer">
      <p>Generated by K6 HTML Report Generator</p>
      <p style="margin-top: 5px; opacity: 0.8;">Meeting Room Booking System Performance Testing Suite</p>
    </div>
  </div>
</body>
</html>
  `;

  // Compile and render template
  const compiledTemplate = Handlebars.compile(template);
  const html = compiledTemplate({
    summary,
    tests: testResults
  });

  // Write HTML file
  fs.writeFileSync(outputFile, html);
  console.log(`✅ HTML report generated: ${outputFile}`);
  console.log(`🌐 Open in browser: file://${path.resolve(outputFile)}`);
}

// Main execution
if (require.main === module) {
  const args = process.argv.slice(2);
  
  if (args.length === 0) {
    console.log('Usage: node generate-html-report.js <results-dir> [output-file]');
    console.log('Example: node generate-html-report.js ./results/20231029_120000 ./results/20231029_120000/report.html');
    process.exit(1);
  }

  const resultsDir = args[0];
  const outputFile = args[1] || path.join(resultsDir, 'report.html');

  if (!fs.existsSync(resultsDir)) {
    console.error(`❌ Results directory not found: ${resultsDir}`);
    process.exit(1);
  }

  generateReport(resultsDir, outputFile)
    .then(() => {
      console.log('✨ Report generation complete');
      process.exit(0);
    })
    .catch(error => {
      console.error('❌ Error generating report:', error);
      process.exit(1);
    });
}

module.exports = { generateReport, parseK6Results, aggregateMetrics };
