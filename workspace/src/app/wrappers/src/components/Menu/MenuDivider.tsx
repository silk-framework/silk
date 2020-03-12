import React from 'react';
import { MenuDivider as BlueprintMenuDivider } from "@blueprintjs/core";

const MenuDivider = ({children, className='', ...restProps}: any) => <BlueprintMenuDivider {...restProps} className={'ecc-menu__divider '+className}>{children}</BlueprintMenuDivider>;

export default MenuDivider;
