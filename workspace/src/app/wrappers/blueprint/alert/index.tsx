import React, { memo, ReactElement } from 'react';
import { Alert as B_Alert } from "@blueprintjs/core";

function Alert({children, ...restProps}: any): ReactElement<B_Alert> {
    return <B_Alert {...restProps}>{children}</B_Alert>
}

export default memo(Alert);
