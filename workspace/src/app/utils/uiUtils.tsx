import { Tooltip, ContextOverlayProps } from "@eccenca/gui-elements";
import React from "react";

/** Wraps an element inside a tooltip when the wrap predicate is true. */
export const wrapTooltip = (
    wrapPredicate: boolean,
    childTooltip: string | React.JSX.Element,
    child: React.JSX.Element,
    position: ContextOverlayProps["placement"] = "bottom-start",
    size: "large" | "small" | "medium" = "large",
): React.JSX.Element => {
    if (wrapPredicate) {
        return (
            <Tooltip content={childTooltip} placement={position} size={size} minimal>
                {child}
            </Tooltip>
        );
    } else {
        return child;
    }
};
