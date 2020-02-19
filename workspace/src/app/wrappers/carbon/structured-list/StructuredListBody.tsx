import React, { memo } from 'react';
import {
    StructuredListBody as C_StructuredListBody,
    StructuredListBodyProps
} from "carbon-components-react";

const StructuredListBody = memo((props: StructuredListBodyProps) => {
    return (
        <C_StructuredListBody {...props}>
            {props.children}
        </C_StructuredListBody>
    );
});

export default StructuredListBody;
