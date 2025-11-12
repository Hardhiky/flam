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

    // Log initialization complete
    console.log("Edge Detector Web Viewer initialized successfully");
    console.log('Press "L" to load sample, "R" to refresh, "C" to clear');
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
   * Initialize WebSocket connection (for future implementation)
   */
  // @ts-ignore - Reserved for future use
  private initializeWebSocket(): void {
    console.log(
      "WebSocket initialization (placeholder for future implementation)",
    );
    // const ws = new WebSocket('ws://localhost:8080/stream');
    // ws.onopen = () => console.log('WebSocket connected');
    // ws.onmessage = (event) => this.handleWebSocketMessage(event);
    // ws.onerror = (error) => console.error('WebSocket error:', error);
    // ws.onclose = () => console.log('WebSocket closed');
  }

  /**
   * Handle WebSocket message (placeholder)
   */
  // @ts-ignore - Reserved for future use
  private handleWebSocketMessage(event: MessageEvent): void {
    try {
      const data = JSON.parse(event.data);
      console.log("Received WebSocket data:", data);

      // Expected format:
      // {
      //   imageData: "base64...",
      //   width: 1280,
      //   height: 720,
      //   mode: 1,
      //   processingTime: 25,
      //   timestamp: 1234567890
      // }

      if (data.imageData && data.width && data.height) {
        this.frameViewer.loadFrameFromBase64(
          data.imageData,
          data.width,
          data.height,
          data.mode || ProcessingMode.EDGE,
        );
      }
    } catch (error) {
      const errorMessage =
        error instanceof Error ? error.message : "Unknown error";
      console.error("Error handling WebSocket message:", errorMessage);
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
