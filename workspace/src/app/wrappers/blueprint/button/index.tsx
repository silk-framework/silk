import React, { memo, ReactElement } from 'react';
import { Button as B_Button } from "@blueprintjs/core";

const Button = memo(({children, ...restProps}: any): ReactElement<B_Button> =>
    <B_Button {...restProps}>{children}</B_Button>
);

export default Button;
