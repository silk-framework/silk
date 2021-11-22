import React from 'react';
import { CardContent, Info } from '@eccenca/gui-elements';

const EmptyList = () => {
    return (
        <CardContent>
            <Info vertSpacing border>
                No existing mapping rules.
            </Info>
            {/* TODO: we should provide options like adding rules or suggestions here,
                         even a help text would be a good support for the user.
                         */}
        </CardContent>
    )
};

export default EmptyList;
