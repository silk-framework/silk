import React, { memo } from 'react';
import { StructuredListCell as C_StructuredListCell, StructuredListCellProps } from "carbon-components-react";

const StructuredListCell = memo((props: StructuredListCellProps) => {
    return (
        <C_StructuredListCell {...props}>
            {props.children}
        </C_StructuredListCell>
    );
});

export default StructuredListCell;
