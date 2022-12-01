import { ContextOverlayProps, Tooltip } from "@eccenca/gui-elements";
import React from "react";

/** Wraps an element inside a tooltip when the wrap predicate is true. */
export const wrapTooltip = (
    wrapPredicate: boolean,
    childTooltip: string | JSX.Element,
    child: JSX.Element,
    position: ContextOverlayProps["placement"] = "bottom-start",
    size: "large" | "small" | "medium" = "large"
): JSX.Element => {
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
