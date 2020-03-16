import React from 'react';
import {
    Button as BlueprintButton,
    AnchorButton as BlueprintAnchorButton,
    Intent as BlueprintIntent,
 } from "@blueprintjs/core";
import Icon from '../Icon/Icon';

const Button = ({
    children,
    className='',
    affirmative=false,
    disruptive=false,
    elevated=false,
    href=false,
    icon=false,
    rightIcon=false,
    ...restProps
}: any) => {

    let intention;
    if (affirmative || elevated) intention = BlueprintIntent.PRIMARY;
    if (disruptive) intention = BlueprintIntent.DANGER;

    let ButtonType = (href) ? BlueprintAnchorButton : BlueprintButton;

    return (
        <ButtonType
            {...restProps}
            className={'ecc-button '+className}
            intent={intention}
            icon={
                typeof icon === 'string' ? <Icon name={icon} /> : icon
            }
            rightIcon={
                typeof rightIcon === 'string' ? <Icon name={rightIcon} /> : rightIcon
            }
            href
        >
            {children}
        </ButtonType>
    );
};

export default Button;
