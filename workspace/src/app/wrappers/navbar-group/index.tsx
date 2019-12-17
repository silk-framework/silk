import React, { memo } from 'react';
import { NavbarGroup as B_NavbarGroup } from "@blueprintjs/core";

const NavbarGroup = memo(({...restProps}: any) =>
    <B_NavbarGroup {...restProps}/>
);

export default NavbarGroup;
