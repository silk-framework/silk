import React from "react";
import { Link } from "@gui-elements/index";

export function ResourceLink({ children, handlerResourcePageLoader, handlerResourceQuickInformationLoader, url }: any) {
    const stopEvent = (event) => {
        // Do not stop event if user has pressed CTRL key
        if (!event.ctrlKey) {
            event.preventDefault();
            event.nativeEvent.stopImmediatePropagation();
            event.stopPropagation();
        }
    };

    const handlerSingleClick = (event) => {
        if (handlerResourceQuickInformationLoader) {
            stopEvent(event);
            handlerResourceQuickInformationLoader(event);
        } else {
            handlerDoubleClick(event);
        }
    };

    const handlerDoubleClick = (event) => {
        if (handlerResourcePageLoader) {
            stopEvent(event);
            handlerResourcePageLoader(event);
        }
    };

    return (
        <Link
            disabled={!url && !handlerResourcePageLoader && !handlerResourceQuickInformationLoader}
            href={url ? url : false}
            onClick={handlerSingleClick}
            onDoubleClick={handlerDoubleClick}
        >
            {children}
        </Link>
    );
}
