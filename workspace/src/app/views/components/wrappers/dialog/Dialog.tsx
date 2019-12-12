import React, { memo } from "react";
import { Dialog as B_Dialog } from '@blueprintjs/core';

const Dialog = memo(({children, ...restProps}: any) => {
    return (
        <B_Dialog
            {...restProps}
        >
            {children}
        </B_Dialog>
    )
});

export default Dialog;
