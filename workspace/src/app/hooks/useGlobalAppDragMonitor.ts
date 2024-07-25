import React from "react";

const APP_DRAG_OVER_CLASS = "appDragOver";
const nonClassChars = /[^-_a-zA-Z0-9]/g;

/** Tracks drag operations over the application. Sets different classes to the root div. */
export const useGlobalAppDragMonitor = () => {
    React.useEffect(() => {
        const rootDiv = document.getElementById("root");
        let currentTimer: any = undefined;
        let currentClass: string | undefined = undefined;

        const removeDragClass = () => {
            currentClass && rootDiv?.classList.remove(currentClass);
        };

        const onDragOver = (event) => {
            if (currentTimer) {
                clearTimeout(currentTimer);
            }
            const types = new Set(event.dataTransfer.types);
            if (types.size === 1) {
                const type = types.values().next().value;
                currentClass = `${APP_DRAG_OVER_CLASS}_${type}`.replaceAll(nonClassChars, "");
                rootDiv?.classList.add(currentClass);
                currentTimer = setTimeout(removeDragClass, 500);
            }
        };

        if (rootDiv) {
            rootDiv.addEventListener("dragover", onDragOver);
        }
    }, []);
};
