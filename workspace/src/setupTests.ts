import "regenerator-runtime/runtime";
import "@testing-library/jest-dom";
import { TextEncoder, TextDecoder } from "util";

jest.setTimeout(30000);

if (window.document) {
    window.document.createRange = () => {
        const range = new Range();
        range.getBoundingClientRect = jest.fn(() => ({
            bottom: 0,
            height: 0,
            left: 0,
            right: 0,
            top: 0,
            width: 0,
            x: 0,
            y: 0,
            toJSON: () => {},
        }));
        range.getClientRects = jest.fn(() => {
            const rects: any = [];
            rects.item = () => null;
            rects.length = 0;
            return rects;
        });
        return range;
    };
}

global.ResizeObserver = jest.fn().mockImplementation(() => ({
    observe: jest.fn(),
    unobserve: jest.fn(),
    disconnect: jest.fn(),
}));

Object.defineProperty(window, "matchMedia", {
    writable: true,
    value: jest.fn().mockImplementation((query) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: jest.fn(), // Deprecated
        removeListener: jest.fn(), // Deprecated
        addEventListener: jest.fn(),
        removeEventListener: jest.fn(),
        dispatchEvent: jest.fn(),
    })),
});

// Mock window.scrollTo and related scroll methods
Object.defineProperty(window, "scrollTo", {
    writable: true,
    value: jest.fn(),
});

// Mock Element.prototype.scrollIntoView for useScrollIntoView hook
Element.prototype.scrollIntoView = jest.fn();

// Mock window.scrollY for scroll offset calculations
Object.defineProperty(window, "scrollY", {
    writable: true,
    value: 0,
    configurable: true,
});

// --- jsdom polyfills for Radix UI primitives (guarded) -----------------------------------------
// Radix relies on PointerEvent + pointer capture APIs that jsdom does not implement.

if (typeof window !== "undefined" && typeof window.PointerEvent === "undefined") {
    class PointerEventPolyfill extends MouseEvent {
        public pointerId: number;
        public width: number;
        public height: number;
        public pressure: number;
        public tangentialPressure: number;
        public tiltX: number;
        public tiltY: number;
        public twist: number;
        public pointerType: string;
        public isPrimary: boolean;

        constructor(type: string, params: PointerEventInit = {}) {
            super(type, params);
            this.pointerId = params.pointerId ?? 0;
            this.width = params.width ?? 1;
            this.height = params.height ?? 1;
            this.pressure = params.pressure ?? 0;
            this.tangentialPressure = params.tangentialPressure ?? 0;
            this.tiltX = params.tiltX ?? 0;
            this.tiltY = params.tiltY ?? 0;
            this.twist = params.twist ?? 0;
            this.pointerType = params.pointerType ?? "mouse";
            this.isPrimary = params.isPrimary ?? false;
        }
    }
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (window as any).PointerEvent = PointerEventPolyfill;
}

if (typeof Element !== "undefined") {
    if (!Element.prototype.hasPointerCapture) {
        Element.prototype.hasPointerCapture = jest.fn().mockReturnValue(false);
    }
    if (!Element.prototype.setPointerCapture) {
        Element.prototype.setPointerCapture = jest.fn();
    }
    if (!Element.prototype.releasePointerCapture) {
        Element.prototype.releasePointerCapture = jest.fn();
    }
}
