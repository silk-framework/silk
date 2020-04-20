import React from 'react';

function CardActionsAux ({
    children,
    className='',
    ...otherProps
}: any) {
    return (
        <div
            {...otherProps}
            className={
                'ecc-card__actions__aux' +
                (className ? ' ' + className : '')
            }
        >
            {children}
        </div>
    );
};

export default CardActionsAux;
