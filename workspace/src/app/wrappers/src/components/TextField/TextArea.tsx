import React from 'react';
import {
    TextArea as BlueprintTextArea,
    Intent as BlueprintIntent,
} from "@blueprintjs/core";

function TextArea({
    className='',
    hasStatePrimary=false,
    hasStateSuccess=false,
    hasStateWarning=false,
    hasStateDanger=false,
    fullWidth=false,
    ...otherProps
}: any) {

    let intent;
    switch (true) {
        case hasStatePrimary:
            intent = BlueprintIntent.PRIMARY;
            break;
        case hasStateSuccess:
            intent = BlueprintIntent.SUCCESS;
            break;
        case hasStateWarning:
            intent = BlueprintIntent.WARNING;
            break;
        case hasStateDanger:
            intent = BlueprintIntent.DANGER;
            break;
        default:
            break;
    }

    return (
        <BlueprintTextArea
            className={'ecc-textarea ' + className}
            intent={intent}
            fill={fullWidth}
            {...otherProps}
            dir={'auto'}
        />
    );
};

export default TextArea;
