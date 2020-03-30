import React from 'react';
import { Menu as BlueprintMenu } from "@blueprintjs/core";

function Menu({children, className='', ...restProps}: any) {
    return <BlueprintMenu {...restProps} className={'ecc-menu__list '+className}>{children}</BlueprintMenu>;
}

export default Menu;
