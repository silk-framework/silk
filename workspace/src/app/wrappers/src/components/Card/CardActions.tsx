import React from 'react';

function CardActions ({
    children,
    className='',
    inverseDirection=false,
    ...otherProps
}: any) {
    return (
        <footer
            {...otherProps}
            className={
                'ecc-card__actions' +
                (inverseDirection ? ' ecc-card__actions--inversedirection' : '') +
                (className ? ' ' + className : '')
            }
        >
            {children}
        </footer>
    );
};

export default CardActions;
