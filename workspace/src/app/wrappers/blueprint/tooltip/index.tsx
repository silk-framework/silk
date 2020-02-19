import React, { memo } from 'react';
import { Tooltip as B_Tooltip } from "@blueprintjs/core";

const Tooltip = memo(({children, ...restProps}: any) =>
    <B_Tooltip {...restProps}>{children}</B_Tooltip>
);

export default Tooltip;
