/** Contains utility functions for the dnd-kit drag */

// Block DnD event propagation if element has "nodrag" class
const preventDraggingInNoDragElements = (targetElement: HTMLElement ) => {
    let cur = targetElement
    while (cur) {
        if (cur.classList.contains("nodrag")) {
            return false;
        }
        cur = cur.parentElement as HTMLElement;
    }

    return true;
};

const exportObject = {
    preventDraggingInNoDragElements,
}

export default exportObject
