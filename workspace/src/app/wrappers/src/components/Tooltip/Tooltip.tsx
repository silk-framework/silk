import React from "react";
import { Classes as BlueprintClassNames, Tooltip as BlueprintTooltip } from "@blueprintjs/core";

function Tooltip({ children, className = "", addIndicator = false, ...otherProps }: any) {
    return (
        <BlueprintTooltip
            lazy={true}
            hoverOpenDelay={500}
            {...otherProps}
            className={
                "ecc-tooltip__wrapper" +
                (className ? " " + className : "") +
                (addIndicator === true ? " " + BlueprintClassNames.TOOLTIP_INDICATOR : "")
            }
            targetClassName={"ecc-tooltip__target" + (className ? " " + className + "__target" : "")}
            popoverClassName={"ecc-tooltip__content" + (className ? " " + className + "__content" : "")}
        >
            {children}
        </BlueprintTooltip>
    );
}

export default Tooltip;
