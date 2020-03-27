import React, { memo } from 'react';
import { InputGroup as B_InputGroup } from "@blueprintjs/core";

const InputGroup = memo(({...restProps}: any) => {
    return <B_InputGroup {...restProps}/>
});

export default InputGroup;
