/**
 * Simulate Android app sending frames via WebSocket
 * This script connects to the WebSocket server and sends test frames
 */

const WebSocket = require('ws');

const WS_URL = 'ws://localhost:8080';
const FRAME_INTERVAL = 100; // Send frame every 100ms (10 FPS)
const MODES = {
  RAW: 0,
  EDGE: 1,
  GRAYSCALE: 2
};

let frameCounter = 0;
let ws = null;

// Generate a test frame (colored square pattern)
function generateTestFrame(mode) {
  // Create a simple 100x100 pixel test pattern
  const width = 640;
  const height = 480;

  // Generate different patterns based on mode
  let canvas;
  if (typeof document !== 'undefined') {
    canvas = document.createElement('canvas');
  } else {
    // Node.js environment - use a placeholder base64 image
    const images = {
      [MODES.RAW]: generateColorPattern(),
      [MODES.EDGE]: generateEdgePattern(),
      [MODES.GRAYSCALE]: generateGrayscalePattern()
    };
    return images[mode];
  }

  canvas.width = width;
  canvas.height = height;
  const ctx = canvas.getContext('2d');

  // Draw pattern based on mode
  switch (mode) {
    case MODES.RAW:
      // Colorful pattern
      ctx.fillStyle = `rgb(${frameCounter % 255}, ${(frameCounter * 2) % 255}, ${(frameCounter * 3) % 255})`;
      ctx.fillRect(0, 0, width, height);
      break;
    case MODES.EDGE:
      // White edges on black background
      ctx.fillStyle = 'black';
      ctx.fillRect(0, 0, width, height);
      ctx.strokeStyle = 'white';
      ctx.lineWidth = 3;
      ctx.strokeRect(50, 50, width - 100, height - 100);
      break;
    case MODES.GRAYSCALE:
      // Grayscale gradient
      const gradient = ctx.createLinearGradient(0, 0, width, 0);
      gradient.addColorStop(0, 'black');
      gradient.addColorStop(1, 'white');
      ctx.fillStyle = gradient;
      ctx.fillRect(0, 0, width, height);
      break;
  }

  return canvas.toDataURL('image/png');
}

// Generate simple color pattern (Node.js compatible)
function generateColorPattern() {
  // Generate a gradient PNG
  return 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAYAAACNMs+9AAAAFUlEQVR42mP8z8BQz0AEYBxVSF+FABJADveWkH6oAAAAAElFTkSuQmCC';
}

function generateEdgePattern() {
  // Simple edge detection pattern
  return 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAYAAACNMs+9AAAAFklEQVR42mNgYGD4z0AEYBxVOKoQpwIARB4D/L2dBRsAAAAASUVORK5CYII=';
}

function generateGrayscalePattern() {
  // Grayscale pattern
  return 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAYAAACNMs+9AAAAFklEQVR42mNgYGD4TwBgHFU4qhCnAgBEHgP8vZ0FGwAAAABJRU5ErkJggg==';
}

// Connect to WebSocket server
function connect() {
  console.log('🔌 Connecting to WebSocket server:', WS_URL);

  ws = new WebSocket(WS_URL);

  ws.on('open', () => {
    console.log('✅ Connected to WebSocket server');
    console.log('📡 Starting frame transmission...');
    startSendingFrames();
  });

  ws.on('message', (data) => {
    try {
      const message = JSON.parse(data.toString());
      handleMessage(message);
    } catch (error) {
      console.error('❌ Error parsing message:', error.message);
    }
  });

  ws.on('error', (error) => {
    console.error('❌ WebSocket error:', error.message);
  });

  ws.on('close', () => {
    console.log('👋 Disconnected from WebSocket server');
    console.log('🔄 Reconnecting in 5 seconds...');
    setTimeout(connect, 5000);
  });
}

// Handle messages from server
function handleMessage(message) {
  switch (message.type) {
    case 'connection':
      console.log(`📡 ${message.message}`);
      console.log(`👥 Connected clients: ${message.clientCount}`);
      break;
    case 'pong':
      console.log('🏓 Pong received');
      break;
    case 'stats':
      console.log('📊 Server stats:', message.data);
      break;
    default:
      console.log('📨 Received message:', message.type);
  }
}

// Start sending frames
function startSendingFrames() {
  setInterval(() => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      sendFrame();
    }
  }, FRAME_INTERVAL);
}

// Send a single frame
function sendFrame() {
  frameCounter++;

  // Cycle through different modes
  const mode = frameCounter % 30 < 10 ? MODES.RAW :
               frameCounter % 30 < 20 ? MODES.EDGE :
               MODES.GRAYSCALE;

  const frameData = {
    type: 'frame',
    imageData: generateTestFrame(mode),
    width: 640,
    height: 480,
    mode: mode,
    processingTime: Math.floor(Math.random() * 30) + 10, // 10-40ms
    timestamp: Date.now(),
    frameNumber: frameCounter
  };

  ws.send(JSON.stringify(frameData));

  const modeName = Object.keys(MODES).find(key => MODES[key] === mode);
  console.log(`📤 Frame #${frameCounter} sent (${modeName} mode, ${frameData.processingTime}ms)`);
}

// Send ping to keep connection alive
function sendPing() {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({
      type: 'ping',
      timestamp: Date.now()
    }));
  }
}

// Main execution
console.log('='.repeat(60));
console.log('🤖 Android Frame Simulator');
console.log('='.repeat(60));
console.log(`Target: ${WS_URL}`);
console.log(`Frame Rate: ${1000 / FRAME_INTERVAL} FPS`);
console.log(`Modes: RAW → EDGE → GRAYSCALE (cycling)`);
console.log('='.repeat(60));

// Connect to server
connect();

// Send ping every 30 seconds
setInterval(sendPing, 30000);

// Handle graceful shutdown
process.on('SIGINT', () => {
  console.log('\n\n👋 Shutting down simulator...');
  if (ws) {
    ws.close(1000, 'Simulator shutting down');
  }
  console.log('✅ Goodbye!');
  process.exit(0);
});

process.on('SIGTERM', () => {
  console.log('\n\n👋 Shutting down simulator...');
  if (ws) {
    ws.close(1000, 'Simulator shutting down');
  }
  console.log('✅ Goodbye!');
  process.exit(0);
});
