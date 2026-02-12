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
            toJSON: () => {}
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
