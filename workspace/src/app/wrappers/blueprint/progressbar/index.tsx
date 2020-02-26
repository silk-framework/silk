import React, { memo, ReactElement } from 'react';
import { ProgressBar as B_ProgressBar } from "@blueprintjs/core";

const ProgressBar = memo(({...restProps}: any): ReactElement<B_ProgressBar> =>
    <B_ProgressBar {...restProps} />
);

export default ProgressBar;
