import React from 'react';

const CardContent = ({
    children,
    className='',
    ...otherProps
}: any) => {
    return (
        <div
            {...otherProps}
            className={'ecc-card__content ' + className}
        >
            {children}
        </div>
    );
};

export default CardContent;
