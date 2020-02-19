import React, { memo } from 'react';
import {
    StructuredListSkeleton as C_StructuredListSkeleton,
    StructuredListSkeletonProps
} from "carbon-components-react";

const StructuredListSkeleton = memo((props: StructuredListSkeletonProps) => {
    return (
        <C_StructuredListSkeleton {...props} />
    );
});

export default StructuredListSkeleton;
