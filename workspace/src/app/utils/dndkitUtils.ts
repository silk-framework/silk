/** Contains utility functions for the dnd-kit drag and drop library.*/

import {MouseSensor} from "@dnd-kit/core";
import {MouseEvent} from "react";

/** Blocks DnD event propagation if element has "nodrag" class */
const preventDraggingInNoDragElements = (targetElement: HTMLElement ): boolean => {
    let cur = targetElement
    while (cur) {
        if (cur.classList.contains("nodrag")) {
            return false;
        }
        cur = cur.parentElement as HTMLElement;
    }

    return true;
}

/** Mouse sensor that does not drag elements having the "nodrag" class attached to them. */
class DefaultMouseSensor extends MouseSensor {
    static activators = [{
        eventName: 'onMouseDown',
        handler: ({ nativeEvent: event }: MouseEvent) => preventDraggingInNoDragElements(event.target as HTMLElement)
    }] satisfies typeof MouseSensor['activators'];
}

const exportObject = {
    preventDraggingInNoDragElements,
    DefaultMouseSensor
}

export default exportObject
