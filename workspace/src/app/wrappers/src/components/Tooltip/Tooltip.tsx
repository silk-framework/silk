import React from "react";
import {
    Tooltip as BlueprintTooltip,
    Classes as BlueprintClassNames,
} from "@blueprintjs/core";

function Tooltip({
    children,
    className='',
    addIndicator=false,
    ...otherProps
}: any) {
    return (
        <BlueprintTooltip
            lazy={true}
            {...otherProps}
            className={
                'ecc-tooltip__wrapper' +
                (className ? ' ' + className : '') +
                (addIndicator === true ? ' ' + BlueprintClassNames.TOOLTIP_INDICATOR : '')
            }
            targetClassName={'ecc-tooltip__target' + (className ? ' '+className+'__target' : '')}
            popoverClassName={'ecc-tooltip__content' + (className ? ' '+className+'__content' : '')}
        >
            {children}
        </BlueprintTooltip>
    );
};

export default Tooltip;
