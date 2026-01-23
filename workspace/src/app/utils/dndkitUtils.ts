/** Contains utility functions for the dnd-kit drag and drop library.*/

import {MouseSensor, MouseSensorOptions, SensorDescriptor} from "@dnd-kit/core";
import {MouseEvent} from "react";

/** Blocks DnD event propagation if element has "nodrag" class. Do not use this for allowing clicks. Clicks should be
 * fixed by adding the distance activationConstraint to the useSensor function.
 * Use this for cases like allowing copying text or input elements.
 * */
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

const defaultMouseSensorOptions: MouseSensorOptions = {
    activationConstraint: {
        distance: 5,
    }
}

const exportObject = {
    preventDraggingInNoDragElements,
    DefaultMouseSensor,
    defaultMouseSensorOptions
}

export default exportObject
