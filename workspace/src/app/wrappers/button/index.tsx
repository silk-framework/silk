import React, { memo } from 'react';
import { Button as B_Button } from "@blueprintjs/core";

const Button = memo(({children, ...restProps}: any) =>
    <B_Button {...restProps}>{children}</B_Button>
);

export default Button;
