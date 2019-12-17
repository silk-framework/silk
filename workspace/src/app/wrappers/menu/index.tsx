import React from 'react';
import { Menu as B_Menu } from "@blueprintjs/core";

const Menu = ({children, ...restProps}: any) => <B_Menu {...restProps}>{children}</B_Menu>;

export default Menu;
