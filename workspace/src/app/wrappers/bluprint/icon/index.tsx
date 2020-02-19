import React, { memo } from 'react';
import { Icon as B_Icon } from "@blueprintjs/core";

const Icon = memo(({children, ...restProps}: any) =>
    <B_Icon {...restProps}>{children}</B_Icon>
);

export default Icon;
