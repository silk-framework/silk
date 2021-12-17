import { Tooltip } from "gui-elements";
import React from "react";

/** Wraps an element inside a tooltip when the wrap predicate is true. */
export const wrapTooltip = (
    wrapPredicate: boolean,
    childTooltip: string | JSX.Element,
    child: JSX.Element,
    position: string = "bottom-left",
    size: "large" | "small" | "medium" = "large"
): JSX.Element => {
    if (wrapPredicate) {
        return (
            <Tooltip content={childTooltip} position={position} size={size} minimal>
                {child}
            </Tooltip>
        );
    } else {
        return child;
    }
};
