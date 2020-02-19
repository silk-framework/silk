import React, { memo } from 'react';
import { NavbarHeading as B_NavbarHeading } from "@blueprintjs/core";

const NavbarHeading = memo(({...restProps}: any) =>
    <B_NavbarHeading {...restProps}/>
);

export default NavbarHeading;
