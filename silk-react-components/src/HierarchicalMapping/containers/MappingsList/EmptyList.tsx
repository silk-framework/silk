import React from 'react';
import { CardContent, Info } from 'gui-elements-deprecated';

const EmptyList = () => {
    return (
        <CardContent>
            <Info vertSpacing border>
                No existing mapping rules.
            </Info>
        </CardContent>
    )
};

export default EmptyList;
