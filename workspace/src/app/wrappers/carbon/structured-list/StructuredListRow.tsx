import React, { memo } from 'react';
import { AllStructuredListRowProps, StructuredListRow as C_StructuredListRow } from "carbon-components-react";

const StructuredListRow = memo((props: AllStructuredListRowProps) => {
    return (
        <C_StructuredListRow {...props}>
            {props.children}
        </C_StructuredListRow>
    );
});

export default StructuredListRow;
