import React from 'react';
export default function Col({ children, className = '', span = 0, size = ''}) {
    let _className = className;
    if (!span) {
        _className += ` bx--col`;
    } else {
        size = size || 'lg';
        _className += ` bx--col-${size}-${span}`;
    }
    return (
        <div className={_className}>{children}</div>
    )
}
