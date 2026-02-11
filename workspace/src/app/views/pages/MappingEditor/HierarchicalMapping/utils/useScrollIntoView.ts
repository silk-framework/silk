import { useRef, useCallback } from "react";
import _ from "lodash";

/**
 * Custom hook that provides scroll-into-view functionality for a component.
 *
 * @returns {Object} An object containing:
 *   - elementRef: Ref to attach to the element you want to scroll into view
 *   - scrollIntoView: Function to scroll the element into view
 *
 * @example
 * const MyComponent = () => {
 *   const { elementRef, scrollIntoView } = useScrollIntoView();
 *
 *   useEffect(() => {
 *     scrollIntoView(); // Scroll this component into view
 *   }, []);
 *
 *   return <div ref={elementRef}>Content</div>;
 * };
 */
export function useScrollIntoView<T extends HTMLElement = HTMLElement>() {
    const elementRef = useRef<T>(null);

    const scrollIntoView = useCallback((topOffset?: number) => {
        if (elementRef.current) {
            const element = elementRef.current;

            // Check if scrollIntoView is available (not available in some test environments)
            if (typeof element.scrollIntoView !== "function") {
                return;
            }

            // Scroll element to top of container
            element.scrollIntoView({
                behavior: "smooth",
                block: "start",
                inline: "nearest",
            });

            // Apply topOffset correction if specified
            if (topOffset && typeof topOffset === "number") {
                // Find the scrollable parent container
                const getScrollParent = (node: HTMLElement): HTMLElement | Window => {
                    let parent = node.parentElement;
                    while (parent) {
                        const { overflow, overflowY } = window.getComputedStyle(parent);
                        if (/(auto|scroll)/.test(overflow + overflowY)) {
                            return parent;
                        }
                        parent = parent.parentElement;
                    }
                    return window;
                };

                // Small delay to ensure scrollIntoView completes
                setTimeout(() => {
                    const scrollParent = getScrollParent(element);

                    if (scrollParent === window) {
                        // Scrolling the window
                        const currentScrollY = window.scrollY || document.documentElement.scrollTop;
                        window.scrollTo({
                            top: currentScrollY - topOffset,
                            behavior: "smooth",
                        });
                    } else {
                        // Scrolling a container element
                        const scrollParentElement = scrollParent as HTMLElement;
                        scrollParentElement.scrollTop -= topOffset;
                    }
                }, 50);
            }
        }
    }, []);

    return {
        elementRef,
        scrollIntoView,
    };
}
