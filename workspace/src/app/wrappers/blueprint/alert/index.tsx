import React, { memo, ReactElement } from 'react';
import { Alert as B_Alert } from "@blueprintjs/core";

const Alert = memo(({children, ...restProps}: any): ReactElement<B_Alert> =>
    <B_Alert {...restProps}>{children}</B_Alert>
);

export default Alert;
