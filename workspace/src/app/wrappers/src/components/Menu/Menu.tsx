import React from 'react';
import { Menu as BlueprintMenu } from "@blueprintjs/core";

const Menu = ({children, className='', ...restProps}: any) => <BlueprintMenu {...restProps} className={'ecc-menu__list '+className}>{children}</BlueprintMenu>;

export default Menu;
