import "regenerator-runtime/runtime";
import "@testing-library/jest-dom";

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
