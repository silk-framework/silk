import React from 'react';
import { Position as BlueprintPosition } from "@blueprintjs/core";
import ContextOverlay from './ContextOverlay';
import Menu from '../Menu/Menu';
import IconButton from '../Icon/IconButton';

const ContextMenu = ({
    children,
    className='',
    togglerElement='item-moremenu',
    togglerText='Show more options',
    ...restProps
}: any) => {

    return (
        <ContextOverlay
            {...restProps}
            className={'ecc-contextmenu ' + className}
        >
            {typeof togglerElement === 'string' ? <IconButton name={togglerElement} text={togglerText} /> : {togglerElement}}
            <Menu>
                {children}
            </Menu>
        </ContextOverlay>
    )
}

export default ContextMenu;
