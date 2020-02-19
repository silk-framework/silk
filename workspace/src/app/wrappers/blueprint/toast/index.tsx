import React, { memo } from 'react';
import { Toast as B_Toast } from "@blueprintjs/core";

const Toast = memo((props: any) =>
    <B_Toast {...props}/>
);

export default Toast;
