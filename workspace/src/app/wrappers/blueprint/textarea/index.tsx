import React, { memo } from 'react';
import { TextArea as B_TextArea } from "@blueprintjs/core";

const TextArea = memo((restProps: any) =>
    <B_TextArea {...restProps} />
);

export default TextArea;
