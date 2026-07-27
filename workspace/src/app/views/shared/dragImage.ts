import React from "react";

/**
 * `DataTransfer.setDragImage`, with the Chromium transformed-ancestor workaround.
 *
 * Chromium mis-computes the native drag-image snapshot region when the dragged element — or any of
 * its ancestors — is CSS-`transform`ed: the snapshot captures neighboring content (or comes out
 * blank / offset). GridBoard positions each tile with `transform: translate(...)`, so any element
 * that renders inside a tile and calls `setDragImage(el, …)` directly is affected. The fix is to
 * snapshot an offscreen *clone* mounted straight on `<body>` — where it has no transformed ancestor
 * — and remove it once the browser has taken its snapshot (next tick, still within this event).
 *
 * See https://bugs.chromium.org/p/chromium/issues/detail?id=1201542.
 *
 * @param event  the `dragstart` event whose `dataTransfer` receives the drag image.
 * @param source the element to snapshot (usually `event.currentTarget`).
 * @param offsetX cursor hotspot X within the image; defaults to the element's horizontal centre.
 * @param offsetY cursor hotspot Y within the image; defaults to the element's vertical centre.
 */
export function setDragImageFromClone(
    event: React.DragEvent,
    source: HTMLElement,
    offsetX?: number,
    offsetY?: number,
): void {
    const rect = source.getBoundingClientRect();
    const clone = source.cloneNode(true) as HTMLElement;
    // Park the clone offscreen on <body> so it has no transformed ancestor to confuse the snapshot.
    clone.style.position = "fixed";
    clone.style.top = "0";
    clone.style.left = "-10000px";
    clone.style.width = `${rect.width}px`;
    clone.style.height = `${rect.height}px`;
    clone.style.margin = "0";
    clone.style.pointerEvents = "none";
    document.body.appendChild(clone);
    event.dataTransfer.setDragImage(clone, offsetX ?? rect.width / 2, offsetY ?? rect.height / 2);
    // The browser captures the drag image synchronously during this event; the clone can go next tick.
    window.setTimeout(() => clone.remove(), 0);
}
