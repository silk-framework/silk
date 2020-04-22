import React from 'react';
import {
    InputGroup as BlueprintInputGroup,
    Classes as BlueprintClassNames,
    Intent as BlueprintIntent,
 } from "@blueprintjs/core";
import Icon from '../Icon/Icon';

function TextField({
    className='',
    hasStatePrimary=false,
    hasStateSuccess=false,
    hasStateWarning=false,
    hasStateDanger=false,
    leftIcon=false,
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
        <BlueprintInputGroup
            className={'ecc-textfield ' + className}
            intent={intent}
            fill={fullWidth}
            {...otherProps}
            leftIcon={
                typeof leftIcon === 'string' ? <Icon name={leftIcon} className={BlueprintClassNames.ICON} intent={intent} /> : leftIcon
            }
            dir={'auto'}
        />
    );
};

export default TextField;
