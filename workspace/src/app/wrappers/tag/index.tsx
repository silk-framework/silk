import React, { memo } from 'react';
import { Tag as B_Tag } from "@blueprintjs/core";

const Tag = memo(({children, ...restProps}: any) =>
    <B_Tag {...restProps}>{children}</B_Tag>
);

export default Tag;
