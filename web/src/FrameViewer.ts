/**
 * FrameViewer - Manages frame display and statistics for the web viewer
 */
export interface FrameData {
    imageData: string; // Base64 encoded image
    width: number;
    height: number;
    mode: ProcessingMode;
    processingTime: number;
    timestamp: number;
}

export enum ProcessingMode {
    RAW = 0,
    EDGE = 1,
    GRAYSCALE = 2
}

export interface FrameStats {
    fps: number;
    resolution: string;
    processingTime: number;
    mode: string;
    frameCount: number;
    dataSize: number;
}

export class FrameViewer {
    private frameDisplay: HTMLImageElement;
    private placeholder: HTMLElement;
    private statusIndicator: HTMLElement;
    private timestampElement: HTMLElement;

    // Stats elements
    private statFps: HTMLElement;
    private statResolution: HTMLElement;
    private statProcessingTime: HTMLElement;
    private statMode: HTMLElement;
    private statFrameCount: HTMLElement;
    private statDataSize: HTMLElement;

    // Frame tracking
    private currentFrame: FrameData | null = null;
    private frameCount: number = 0;
    private lastFrameTime: number = 0;
    private fpsHistory: number[] = [];
    private readonly FPS_HISTORY_SIZE = 30;

    // Callbacks
    public onFrameLoaded?: (frame: FrameData) => void;
    public onError?: (error: string) => void;

    constructor() {
        this.frameDisplay = document.getElementById('frameDisplay') as HTMLImageElement;
        this.placeholder = document.getElementById('placeholder') as HTMLElement;
        this.statusIndicator = document.getElementById('statusIndicator') as HTMLElement;
        this.timestampElement = document.getElementById('timestamp') as HTMLElement;

        // Initialize stats elements
        this.statFps = document.getElementById('statFps') as HTMLElement;
        this.statResolution = document.getElementById('statResolution') as HTMLElement;
        this.statProcessingTime = document.getElementById('statProcessingTime') as HTMLElement;
        this.statMode = document.getElementById('statMode') as HTMLElement;
        this.statFrameCount = document.getElementById('statFrameCount') as HTMLElement;
        this.statDataSize = document.getElementById('statDataSize') as HTMLElement;

        this.validateElements();
    }

    /**
     * Validate that all required DOM elements exist
     */
    private validateElements(): void {
        const elements = [
            this.frameDisplay,
            this.placeholder,
            this.statusIndicator,
            this.timestampElement,
            this.statFps,
            this.statResolution,
            this.statProcessingTime,
            this.statMode,
            this.statFrameCount,
            this.statDataSize
        ];

        const missing = elements.filter(el => !el);
        if (missing.length > 0) {
            console.error('Missing required DOM elements:', missing);
        }
    }

    /**
     * Display a frame
     */
    public displayFrame(frame: FrameData): void {
        if (!frame || !frame.imageData) {
            this.onError?.('Invalid frame data');
            return;
        }

        try {
            // Update current frame
            this.currentFrame = frame;
            this.frameCount++;

            // Display image
            this.frameDisplay.src = frame.imageData;
            this.frameDisplay.style.display = 'block';
            this.placeholder.style.display = 'none';

            // Update status indicator
            this.setActive(true);

            // Update timestamp
            this.updateTimestamp(frame.timestamp);

            // Update stats
            this.updateStats(frame);

            // Calculate and update FPS
            this.updateFps();

            // Trigger callback
            this.onFrameLoaded?.({...frame});

            console.log(`Frame displayed: ${frame.width}x${frame.height}, mode: ${this.getModeName(frame.mode)}`);
        } catch (error) {
            const errorMessage = error instanceof Error ? error.message : 'Unknown error';
            console.error('Error displaying frame:', errorMessage);
            this.onError?.(errorMessage);
        }
    }

    /**
     * Load a sample frame (for demo purposes)
     */
    public loadSampleFrame(): void {
        // Create a sample edge-detected frame (placeholder)
        const canvas = document.createElement('canvas');
        const ctx = canvas.getContext('2d');

        if (!ctx) {
            this.onError?.('Canvas not supported');
            return;
        }

        // Sample dimensions
        const width = 1280;
        const height = 720;
        canvas.width = width;
        canvas.height = height;

        // Draw sample edge detection pattern
        ctx.fillStyle = '#000000';
        ctx.fillRect(0, 0, width, height);

        // Draw white edges pattern
        ctx.strokeStyle = '#FFFFFF';
        ctx.lineWidth = 2;

        // Draw some geometric patterns to simulate edges
        for (let i = 0; i < 10; i++) {
            const x = Math.random() * width;
            const y = Math.random() * height;
            const radius = Math.random() * 100 + 50;

            ctx.beginPath();
            ctx.arc(x, y, radius, 0, Math.PI * 2);
            ctx.stroke();
        }

        // Draw grid pattern
        ctx.strokeStyle = '#FFFFFF';
        ctx.lineWidth = 1;
        for (let x = 0; x < width; x += 50) {
            ctx.beginPath();
            ctx.moveTo(x, 0);
            ctx.lineTo(x, height);
            ctx.stroke();
        }
        for (let y = 0; y < height; y += 50) {
            ctx.beginPath();
            ctx.moveTo(0, y);
            ctx.lineTo(width, y);
            ctx.stroke();
        }

        // Add text overlay
        ctx.fillStyle = '#FFFFFF';
        ctx.font = '48px Arial';
        ctx.fillText('Sample Edge Detection', width / 2 - 250, height / 2);
        ctx.font = '24px Arial';
        ctx.fillText('Simulated frame from Android app', width / 2 - 180, height / 2 + 40);

        // Convert to base64
        const imageData = canvas.toDataURL('image/png');

        // Create frame data
        const frame: FrameData = {
            imageData,
            width,
            height,
            mode: ProcessingMode.EDGE,
            processingTime: Math.floor(Math.random() * 30) + 15, // 15-45ms
            timestamp: Date.now()
        };

        this.displayFrame(frame);
    }

    /**
     * Load frame from URL
     */
    public async loadFrameFromUrl(url: string): Promise<void> {
        try {
            const response = await fetch(url);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const blob = await response.blob();
            const reader = new FileReader();

            reader.onloadend = () => {
                const imageData = reader.result as string;

                // Create image to get dimensions
                const img = new Image();
                img.onload = () => {
                    const frame: FrameData = {
                        imageData,
                        width: img.width,
                        height: img.height,
                        mode: ProcessingMode.EDGE,
                        processingTime: 0,
                        timestamp: Date.now()
                    };
                    this.displayFrame(frame);
                };
                img.src = imageData;
            };

            reader.readAsDataURL(blob);
        } catch (error) {
            const errorMessage = error instanceof Error ? error.message : 'Unknown error';
            console.error('Error loading frame from URL:', errorMessage);
            this.onError?.(errorMessage);
        }
    }

    /**
     * Load frame from base64 string
     */
    public loadFrameFromBase64(base64: string, width: number, height: number, mode: ProcessingMode = ProcessingMode.EDGE): void {
        const frame: FrameData = {
            imageData: base64.startsWith('data:') ? base64 : `data:image/png;base64,${base64}`,
            width,
            height,
            mode,
            processingTime: 0,
            timestamp: Date.now()
        };
        this.displayFrame(frame);
    }

    /**
     * Clear the display
     */
    public clear(): void {
        this.frameDisplay.style.display = 'none';
        this.placeholder.style.display = 'block';
        this.currentFrame = null;
        this.setActive(false);
        this.timestampElement.textContent = '';
        console.log('Frame display cleared');
    }

    /**
     * Refresh the current frame
     */
    public refresh(): void {
        if (this.currentFrame) {
            this.displayFrame(this.currentFrame);
        } else {
            this.loadSampleFrame();
        }
    }

    /**
     * Update stats display
     */
    private updateStats(frame: FrameData): void {
        // Resolution
        this.statResolution.textContent = `${frame.width}x${frame.height}`;

        // Processing time
        this.statProcessingTime.textContent = `${frame.processingTime} ms`;

        // Mode
        this.statMode.textContent = this.getModeName(frame.mode);

        // Frame count
        this.statFrameCount.textContent = this.frameCount.toString();

        // Data size (approximate from base64 length)
        const sizeKB = Math.round(frame.imageData.length * 0.75 / 1024);
        this.statDataSize.textContent = `${sizeKB} KB`;
    }

    /**
     * Update FPS calculation
     */
    private updateFps(): void {
        const currentTime = Date.now();

        if (this.lastFrameTime > 0) {
            const delta = currentTime - this.lastFrameTime;
            const fps = 1000 / delta;

            this.fpsHistory.push(fps);
            if (this.fpsHistory.length > this.FPS_HISTORY_SIZE) {
                this.fpsHistory.shift();
            }

            // Calculate average FPS
            const avgFps = this.fpsHistory.reduce((a, b) => a + b, 0) / this.fpsHistory.length;
            this.statFps.textContent = avgFps.toFixed(1);
        }

        this.lastFrameTime = currentTime;
    }

    /**
     * Update timestamp display
     */
    private updateTimestamp(timestamp: number): void {
        const date = new Date(timestamp);
        const timeString = date.toLocaleTimeString();
        this.timestampElement.textContent = `Last updated: ${timeString}`;
    }

    /**
     * Set active/inactive status
     */
    private setActive(active: boolean): void {
        if (active) {
            this.statusIndicator.classList.remove('status-inactive');
            this.statusIndicator.classList.add('status-active');
        } else {
            this.statusIndicator.classList.remove('status-active');
            this.statusIndicator.classList.add('status-inactive');
        }
    }

    /**
     * Get mode name string
     */
    private getModeName(mode: ProcessingMode): string {
        switch (mode) {
            case ProcessingMode.RAW:
                return 'RAW';
            case ProcessingMode.EDGE:
                return 'EDGE';
            case ProcessingMode.GRAYSCALE:
                return 'GRAYSCALE';
            default:
                return 'UNKNOWN';
        }
    }

    /**
     * Get current stats
     */
    public getStats(): FrameStats {
        const avgFps = this.fpsHistory.length > 0
            ? this.fpsHistory.reduce((a, b) => a + b, 0) / this.fpsHistory.length
            : 0;

        return {
            fps: parseFloat(avgFps.toFixed(1)),
            resolution: this.currentFrame ? `${this.currentFrame.width}x${this.currentFrame.height}` : 'N/A',
            processingTime: this.currentFrame?.processingTime ?? 0,
            mode: this.currentFrame ? this.getModeName(this.currentFrame.mode) : 'N/A',
            frameCount: this.frameCount,
            dataSize: this.currentFrame ? Math.round(this.currentFrame.imageData.length * 0.75 / 1024) : 0
        };
    }

    /**
     * Reset stats
     */
    public resetStats(): void {
        this.frameCount = 0;
        this.fpsHistory = [];
        this.lastFrameTime = 0;
        console.log('Stats reset');
    }

    /**
     * Get current frame
     */
    public getCurrentFrame(): FrameData | null {
        return this.currentFrame ? {...this.currentFrame} : null;
    }

    /**
     * Simulate real-time updates (for demo)
     */
    public startSimulation(intervalMs: number = 33): void {
        setInterval(() => {
            if (this.currentFrame) {
                // Update with slight variations
                const simulatedFrame: FrameData = {
                    ...this.currentFrame,
                    processingTime: Math.floor(Math.random() * 30) + 15,
                    timestamp: Date.now()
                };
                this.displayFrame(simulatedFrame);
            }
        }, intervalMs);
        console.log(`Simulation started with ${intervalMs}ms interval`);
    }
}
