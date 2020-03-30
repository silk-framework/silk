import React from 'react';
import { MenuDivider as BlueprintMenuDivider } from "@blueprintjs/core";

function MenuDivider({children, className='', ...restProps}: any) {
    return <BlueprintMenuDivider {...restProps} className={'ecc-menu__divider '+className}>{children}</BlueprintMenuDivider>;
}

export default MenuDivider;
