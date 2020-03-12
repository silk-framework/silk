import React from 'react';
import {
    Popover as BlueprintPropover,
    Position as BlueprintPosition
} from "@blueprintjs/core";

/*

    @see https://blueprintjs.com/docs/#core/components/popover for list of
    properties

*/

const ContextOverlay = ({
    children,
    className='',
    ...restProps
}: any) => {

    return (
        <BlueprintPropover
            position={BlueprintPosition.BOTTOM}
            {...restProps}
            className={'ecc-contextoverlay ' + className}
        >
            {children}
        </BlueprintPropover>
    )
}

export default ContextOverlay;
