import React from 'react';
import { MenuItem as B_MenuItem } from "@blueprintjs/core";

const MenuItem = ({children, ...restProps}: any) => <B_MenuItem {...restProps}>{children}</B_MenuItem>;

export default MenuItem;
