import { Tooltip } from "@gui-elements/index";
import React from "react";

/** Wraps an element inside a tooltip when the wrap predicate is true. */
export const wrapTooltip = (wrapPredicate: boolean, childTooltip: string, child: JSX.Element): JSX.Element => {
    if (wrapPredicate) {
        return (
            <Tooltip content={childTooltip} position="bottom-left">
                {child}
            </Tooltip>
        );
    } else {
        return child;
    }
};
