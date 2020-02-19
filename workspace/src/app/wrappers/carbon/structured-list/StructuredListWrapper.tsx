import React, { memo } from 'react';
import { StructuredListProps, StructuredListWrapper as C_StructuredListWrapper } from "carbon-components-react";

const StructuredListWrapper = memo((props: StructuredListProps) => {
    return (
        <C_StructuredListWrapper {...props}>
            {props.children}
        </C_StructuredListWrapper>
    );
});

export default StructuredListWrapper;
