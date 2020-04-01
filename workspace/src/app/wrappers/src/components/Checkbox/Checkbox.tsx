import React from 'react';
import { Checkbox as BlueprintCheckbox } from "@blueprintjs/core";

function Checkbox({
    children,
    className='',
    ...restProps
}: any) {
    return (
        <BlueprintCheckbox
            {...restProps}
            className={'ecc-checkbox '+className}
        >
            {children}
        </BlueprintCheckbox>
    );
};

export default Checkbox;
