import React, { memo } from 'react';
import { Spinner as B_Spinner } from "@blueprintjs/core";

const Spinner = memo(({...restProps}: any) =>
    <B_Spinner {...restProps}/>
);

export default Spinner;
