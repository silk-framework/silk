import React, { memo } from 'react';
import { NavbarDivider as B_NavbarDivider } from "@blueprintjs/core";

const NavbarDivider = memo(({...restProps}: any) =>
    <B_NavbarDivider {...restProps}/>
);

export default NavbarDivider;
