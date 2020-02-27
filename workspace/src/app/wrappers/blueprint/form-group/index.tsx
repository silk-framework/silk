import React, { memo } from 'react';
import { FormGroup as B_FormGroup } from "@blueprintjs/core";

const FormGroup = memo(({children, ...restProps}: any) =>
    <B_FormGroup {...restProps}>{children}</B_FormGroup>
);

export default FormGroup;
