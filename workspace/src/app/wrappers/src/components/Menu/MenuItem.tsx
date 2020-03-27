import React from 'react';
import { MenuItem as BlueprintMenuItem } from "@blueprintjs/core";
import Icon from '../Icon/Icon';

function MenuItem ({
    children,
    className='',
    icon=false,
    ...restProps
}: any) {

    return (
        <BlueprintMenuItem
            {...restProps}
            className={'ecc-menu__item '+className}
            icon={
                icon ? <Icon name={icon} /> : false
            }
        >
            {children}
        </BlueprintMenuItem>
    );
}

export default MenuItem;
