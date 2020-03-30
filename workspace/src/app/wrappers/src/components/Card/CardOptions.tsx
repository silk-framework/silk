import React from 'react';
import { OverviewItemActions } from './../OverviewItem';

function CardOptions({
    children,
    className='',
    ...otherProps
}: any) {
    return (
        <OverviewItemActions
            {...otherProps}
            className={
                'ecc-card__options' +
                (className ? ' ' + className : '')
            }
        >
            {children}
        </OverviewItemActions>
    );
};

export default CardOptions;
