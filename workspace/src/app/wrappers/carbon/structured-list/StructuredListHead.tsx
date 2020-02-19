import React, { memo } from 'react';
import {
    StructuredListHead as C_StructuredListHead,
    StructuredListHeadProps
} from "carbon-components-react";

const StructuredListHead = memo((props: StructuredListHeadProps) => {
    return (
        <C_StructuredListHead {...props}>
            {props.children}
        </C_StructuredListHead>
    );
});

export default StructuredListHead;
