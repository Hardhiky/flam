/**
 * Main TypeScript entry point for Edge Detector Web Viewer
 */
import { FrameViewer, ProcessingMode, FrameData } from "./FrameViewer.js";

/**
 * Main application class
 */
class EdgeDetectorWebViewer {
  private frameViewer: FrameViewer;
  private isSimulationRunning: boolean = false;
  private ws: WebSocket | null = null;
  private wsReconnectInterval: number | null = null;
  private isConnected: boolean = false;

  // UI Elements
  private btnLoadSample: HTMLButtonElement;
  private btnRefresh: HTMLButtonElement;
  private btnClear: HTMLButtonElement;

  constructor() {
    console.log("Edge Detector Web Viewer initializing...");

    // Initialize frame viewer
    this.frameViewer = new FrameViewer();

    // Get button elements
    this.btnLoadSample = document.getElementById(
      "btnLoadSample",
    ) as HTMLButtonElement;
    this.btnRefresh = document.getElementById(
      "btnRefresh",
    ) as HTMLButtonElement;
    this.btnClear = document.getElementById("btnClear") as HTMLButtonElement;

    // Setup callbacks
    this.setupCallbacks();

    // Setup event listeners
    this.setupEventListeners();

    // Setup keyboard shortcuts
    this.setupKeyboardShortcuts();

    // Initialize WebSocket connection
    this.initializeWebSocket();

    // Log initialization complete
    console.log("Edge Detector Web Viewer initialized successfully");
    console.log('Press "L" to load sample, "R" to refresh, "C" to clear');
    console.log('Press "W" to toggle WebSocket connection');
  }

  /**
   * Setup frame viewer callbacks
   */
  private setupCallbacks(): void {
    this.frameViewer.onFrameLoaded = (frame: FrameData) => {
      console.log(
        `Frame loaded: ${frame.width}x${frame.height}, mode: ${frame.mode}`,
      );
      this.logStats();
    };

    this.frameViewer.onError = (error: string) => {
      console.error("Frame viewer error:", error);
      this.showNotification("Error: " + error, "error");
    };
  }

  /**
   * Setup UI event listeners
   */
  private setupEventListeners(): void {
    // Load sample button
    this.btnLoadSample.addEventListener("click", () => {
      this.handleLoadSample();
    });

    // Refresh button
    this.btnRefresh.addEventListener("click", () => {
      this.handleRefresh();
    });

    // Clear button
    this.btnClear.addEventListener("click", () => {
      this.handleClear();
    });

    // Window resize handler
    window.addEventListener("resize", () => {
      this.handleResize();
    });

    // Visibility change handler
    document.addEventListener("visibilitychange", () => {
      this.handleVisibilityChange();
    });
  }

  /**
   * Setup keyboard shortcuts
   */
  private setupKeyboardShortcuts(): void {
    document.addEventListener("keydown", (event: KeyboardEvent) => {
      // Ignore if typing in an input field
      if (
        event.target instanceof HTMLInputElement ||
        event.target instanceof HTMLTextAreaElement
      ) {
        return;
      }

      switch (event.key.toLowerCase()) {
        case "l":
          this.handleLoadSample();
          break;
        case "r":
          this.handleRefresh();
          break;
        case "c":
          this.handleClear();
          break;
        case "s":
          this.toggleSimulation();
          break;
        case "d":
          this.downloadCurrentFrame();
          break;
        case "i":
          this.printInfo();
          break;
        case "w":
          this.toggleWebSocket();
          break;
      }
    });
  }

  /**
   * Handle load sample button click
   */
  private handleLoadSample(): void {
    console.log("Loading sample frame...");
    this.frameViewer.loadSampleFrame();
    this.showNotification("Sample frame loaded", "success");
  }

  /**
   * Handle refresh button click
   */
  private handleRefresh(): void {
    console.log("Refreshing frame...");
    this.frameViewer.refresh();
    this.showNotification("Frame refreshed", "info");
  }

  /**
   * Handle clear button click
   */
  private handleClear(): void {
    console.log("Clearing display...");
    this.frameViewer.clear();
    this.showNotification("Display cleared", "info");
  }

  /**
   * Handle window resize
   */
  private handleResize(): void {
    const width = window.innerWidth;
    const height = window.innerHeight;
    console.log(`Window resized: ${width}x${height}`);
  }

  /**
   * Handle visibility change (tab active/inactive)
   */
  private handleVisibilityChange(): void {
    if (document.hidden) {
      console.log("Tab hidden - pausing updates");
    } else {
      console.log("Tab visible - resuming updates");
    }
  }

  /**
   * Toggle simulation mode
   */
  private toggleSimulation(): void {
    if (this.isSimulationRunning) {
      console.log("Simulation already running");
      this.showNotification("Simulation already active", "warning");
    } else {
      console.log("Starting simulation...");
      this.frameViewer.startSimulation(33); // ~30 FPS
      this.isSimulationRunning = true;
      this.showNotification("Simulation started", "success");
    }
  }

  /**
   * Download current frame
   */
  private downloadCurrentFrame(): void {
    const frame = this.frameViewer.getCurrentFrame();
    if (!frame) {
      console.warn("No frame to download");
      this.showNotification("No frame available", "warning");
      return;
    }

    try {
      const link = document.createElement("a");
      link.download = `edge-detector-frame-${Date.now()}.png`;
      link.href = frame.imageData;
      link.click();
      console.log("Frame downloaded");
      this.showNotification("Frame downloaded", "success");
    } catch (error) {
      const errorMessage =
        error instanceof Error ? error.message : "Unknown error";
      console.error("Download failed:", errorMessage);
      this.showNotification("Download failed", "error");
    }
  }

  /**
   * Print debug information
   */
  private printInfo(): void {
    console.log("=== Edge Detector Web Viewer Info ===");
    console.log("Version: 1.0.0");
    console.log("Stats:", this.frameViewer.getStats());
    console.log("Current Frame:", this.frameViewer.getCurrentFrame());
    console.log("Simulation Running:", this.isSimulationRunning);
    console.log("=====================================");
  }

  /**
   * Log current stats
   */
  private logStats(): void {
    const stats = this.frameViewer.getStats();
    console.log("Current Stats:", {
      fps: stats.fps,
      resolution: stats.resolution,
      processingTime: stats.processingTime + "ms",
      mode: stats.mode,
      frameCount: stats.frameCount,
      dataSize: stats.dataSize + "KB",
    });
  }

  /**
   * Show notification (console-based for now)
   */
  private showNotification(
    message: string,
    type: "success" | "error" | "warning" | "info",
  ): void {
    const emoji = {
      success: "✅",
      error: "❌",
      warning: "⚠️",
      info: "ℹ️",
    };

    console.log(`${emoji[type]} ${message}`);

    // Could be extended to show toast notifications in the UI
  }

  /**
   * Initialize WebSocket connection
   */
  private initializeWebSocket(): void {
    const wsUrl = "ws://localhost:8080";
    console.log(`🔌 Connecting to WebSocket server: ${wsUrl}`);

    try {
      this.ws = new WebSocket(wsUrl);

      this.ws.onopen = () => {
        console.log("✅ WebSocket connected successfully");
        this.isConnected = true;
        this.updateConnectionStatus(true);
        this.showNotification("WebSocket connected", "success");

        // Clear reconnect interval if it exists
        if (this.wsReconnectInterval) {
          clearInterval(this.wsReconnectInterval);
          this.wsReconnectInterval = null;
        }
      };

      this.ws.onmessage = (event) => {
        this.handleWebSocketMessage(event);
      };

      this.ws.onerror = (error) => {
        console.error("❌ WebSocket error:", error);
        this.updateConnectionStatus(false);
        this.showNotification("WebSocket connection error", "error");
      };

      this.ws.onclose = () => {
        console.log("👋 WebSocket connection closed");
        this.isConnected = false;
        this.updateConnectionStatus(false);
        this.showNotification("WebSocket disconnected", "warning");

        // Attempt to reconnect after 5 seconds
        if (!this.wsReconnectInterval) {
          this.wsReconnectInterval = window.setInterval(() => {
            console.log("🔄 Attempting to reconnect...");
            this.initializeWebSocket();
          }, 5000);
        }
      };
    } catch (error) {
      const errorMessage =
        error instanceof Error ? error.message : "Unknown error";
      console.error("❌ Failed to create WebSocket:", errorMessage);
      this.showNotification("WebSocket connection failed", "error");
    }
  }

  /**
   * Handle WebSocket message
   */
  private handleWebSocketMessage(event: MessageEvent): void {
    try {
      const data = JSON.parse(event.data);

      switch (data.type) {
        case "connection":
          console.log("📡 Connection established:", data.message);
          console.log(`👥 Connected clients: ${data.clientCount}`);
          break;

        case "frame":
          // Frame data received - display it
          if (data.imageData && data.width && data.height) {
            this.frameViewer.loadFrameFromBase64(
              data.imageData,
              data.width,
              data.height,
              data.mode || ProcessingMode.EDGE,
            );
            console.log(
              `🖼️ Frame #${data.frameNumber || "?"} received via WebSocket`,
            );
          }
          break;

        case "stats":
          console.log("📊 Server stats:", data.data);
          break;

        case "pong":
          console.log("🏓 Pong received");
          break;

        default:
          console.log("📨 Received WebSocket message:", data);
      }
    } catch (error) {
      const errorMessage =
        error instanceof Error ? error.message : "Unknown error";
      console.error("❌ Error handling WebSocket message:", errorMessage);
    }
  }

  /**
   * Start periodic frame fetching (REST API alternative)
   */
  public startPolling(url: string, intervalMs: number = 1000): void {
    console.log(`Starting polling from ${url} every ${intervalMs}ms`);

    setInterval(async () => {
      try {
        const response = await fetch(url);
        if (response.ok) {
          const data = await response.json();
          if (data.imageData) {
            this.frameViewer.loadFrameFromBase64(
              data.imageData,
              data.width || 1280,
              data.height || 720,
              data.mode || ProcessingMode.EDGE,
            );
          }
        }
      } catch (error) {
        // Silently fail for polling errors
        console.debug("Polling error:", error);
      }
    }, intervalMs);
  }

  /**
   * Toggle WebSocket connection
   */
  private toggleWebSocket(): void {
    if (this.isConnected && this.ws) {
      console.log("🔌 Disconnecting WebSocket...");
      this.ws.close();
      this.showNotification("WebSocket disconnected", "info");
    } else {
      console.log("🔌 Connecting WebSocket...");
      this.initializeWebSocket();
    }
  }

  /**
   * Update connection status indicator in UI
   */
  private updateConnectionStatus(connected: boolean): void {
    const statusIndicator = document.getElementById("statusIndicator");
    if (statusIndicator) {
      if (connected) {
        statusIndicator.classList.remove("status-inactive");
        statusIndicator.classList.add("status-active");
      } else {
        statusIndicator.classList.remove("status-active");
        statusIndicator.classList.add("status-inactive");
      }
    }
  }

  /**
   * Send ping to server
   */
  public sendPing(): void {
    if (this.ws && this.isConnected) {
      this.ws.send(
        JSON.stringify({
          type: "ping",
          timestamp: Date.now(),
        }),
      );
      console.log("🏓 Ping sent");
    } else {
      console.warn("⚠️ WebSocket not connected");
    }
  }

  /**
   * Request stats from server
   */
  public requestStats(): void {
    if (this.ws && this.isConnected) {
      this.ws.send(
        JSON.stringify({
          type: "stats_request",
          timestamp: Date.now(),
        }),
      );
      console.log("📊 Stats requested");
    } else {
      console.warn("⚠️ WebSocket not connected");
    }
  }

  /**
   * Send frame data to server (for testing)
   */
  public sendTestFrame(): void {
    if (this.ws && this.isConnected) {
      const currentFrame = this.frameViewer.getCurrentFrame();
      if (currentFrame) {
        this.ws.send(
          JSON.stringify({
            type: "frame",
            imageData: currentFrame.imageData,
            width: currentFrame.width,
            height: currentFrame.height,
            mode: currentFrame.mode,
            processingTime: currentFrame.processingTime,
            timestamp: Date.now(),
          }),
        );
        console.log("📤 Test frame sent to server");
        this.showNotification("Test frame sent", "success");
      } else {
        console.warn("⚠️ No frame available to send");
        this.showNotification("No frame to send", "warning");
      }
    } else {
      console.warn("⚠️ WebSocket not connected");
      this.showNotification("WebSocket not connected", "warning");
    }
  }
}

/**
 * Initialize application when DOM is ready
 */
document.addEventListener("DOMContentLoaded", () => {
  console.log("DOM loaded, initializing application...");
  const app = new EdgeDetectorWebViewer();

  // Expose to window for debugging
  (window as any).edgeDetectorApp = app;

  console.log("Application ready!");
  console.log("Available keyboard shortcuts:");
  console.log("  L - Load sample frame");
  console.log("  R - Refresh current frame");
  console.log("  C - Clear display");
  console.log("  S - Start simulation");
  console.log("  D - Download current frame");
  console.log("  I - Print debug info");
  console.log("  W - Toggle WebSocket connection");
  console.log("");
  console.log("WebSocket methods (via console):");
  console.log("  edgeDetectorApp.sendPing()      - Send ping to server");
  console.log("  edgeDetectorApp.requestStats()  - Request server stats");
  console.log(
    "  edgeDetectorApp.sendTestFrame() - Send current frame to server",
  );
});

/**
 * Handle any uncaught errors
 */
window.addEventListener("error", (event: ErrorEvent) => {
  console.error("Uncaught error:", event.error);
});

/**
 * Handle unhandled promise rejections
 */
window.addEventListener(
  "unhandledrejection",
  (event: PromiseRejectionEvent) => {
    console.error("Unhandled promise rejection:", event.reason);
  },
);

// Export for potential module usage
export { EdgeDetectorWebViewer };
