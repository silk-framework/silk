import React from 'react';
import { Radio as BlueprintRadioButton } from "@blueprintjs/core";

function RadioButton({
    children,
    className='',
    ...restProps
}: any) {
    return (
        <BlueprintRadioButton
            {...restProps}
            className={'ecc-radiobutton '+className}
        >
            {children}
        </BlueprintRadioButton>
    );
};

export default RadioButton;
