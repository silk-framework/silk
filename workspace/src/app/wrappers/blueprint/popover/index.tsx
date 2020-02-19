import React, { memo } from 'react';
import { Popover as B_Popover } from "@blueprintjs/core";

const Popover = memo(({children, ...restProps}: any) =>
    <B_Popover {...restProps}>{children}</B_Popover>
);

export default Popover;
