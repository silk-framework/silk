import React from 'react';

export default function Row({children, className = '', ...restProps}) {
    return (
        <div className={`bx--row ${className}`} {...restProps}>
            {children}
        </div>
    )
}
