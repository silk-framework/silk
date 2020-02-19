import React from 'react';

export default function Grid({children, className = '', fullWidth = true, ...restProps}) {
    let _className = `bx-grid`;
    if (fullWidth) {
        _className += `--full-width`;
    }
    _className += ` ${className}`;
    return (
        <div className={_className} {...restProps}>
            {children}
        </div>
    )
}
