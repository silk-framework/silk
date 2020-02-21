import React, { memo } from 'react';
import {
    Pagination as C_Pagination,
    PaginationProps
} from "carbon-components-react";

const PaginationC = memo((props: PaginationProps) => {
    return (
        <C_Pagination {...props}>
            {props.children}
        </C_Pagination>
    );
});

export default PaginationC;