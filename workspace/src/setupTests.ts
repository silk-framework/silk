import "regenerator-runtime/runtime";
import "@testing-library/jest-dom";
import { TextEncoder, TextDecoder } from "util";

(global as any).TextEncoder = TextEncoder;
(global as any).TextDecoder = TextDecoder;

jest.setTimeout(30000);

if (window.document) {
    (window.document.body as any).createTextRange = function () {
        return {
            setEnd: function () {},
            setStart: function () {},
            getBoundingClientRect: function () {
                return { right: 0 };
            },
            getClientRects: function () {
                return {
                    length: 0,
                    left: 0,
                    right: 0,
                };
            },
        };
    };

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
        range.getClientRects = () => ({
            item: () => null,
            length: 0,
            [Symbol.iterator]: jest.fn(),
        } as any);
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
