import React, { memo } from 'react';
import { Navbar as B_Navbar } from "@blueprintjs/core";

const Navbar = memo(({...restProps}: any) =>
    <B_Navbar {...restProps}/>
);

export default Navbar;
