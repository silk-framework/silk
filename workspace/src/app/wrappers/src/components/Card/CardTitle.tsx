import React from 'react';
import { OverviewItemLine } from './../OverviewItem';

function CardTitle({
    children,
    className='',
    narrowed=false,
    ...otherProps
}: any) {
    return (
        <OverviewItemLine
            {...otherProps}
            className={'ecc-card__title ' + className}
            large={!narrowed}
        >
            {children}
        </OverviewItemLine>
    );
};

export default CardTitle;
