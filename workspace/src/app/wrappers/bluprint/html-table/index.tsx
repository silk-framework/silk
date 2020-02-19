import React, { memo } from 'react';
import { HTMLTable as B_HTMLTable } from "@blueprintjs/core";

const HTMLTable = memo(({children, ...restProps}: any) =>
    <B_HTMLTable {...restProps}>{children}</B_HTMLTable>
);

export default HTMLTable;
