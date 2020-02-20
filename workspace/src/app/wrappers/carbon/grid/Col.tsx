import React from 'react';

type ScreenSizes = 'max' | 'xlg' | 'lg' | 'md' | 'sm';

interface IProps {
    children: any,
    /**
     * Column offset
     */
    offset?: number;
    /**
     * Column size
     */
    span?: number;
    /**
     * Breakpoint    Value (px/rem)    Columns    Size (%)    Size    Padding    Margin
     * Small    320 / 20    4    25%    80 px    16 px    0
     * Medium    672 / 42    8    12.5%    80 px    16 px    16 px
     * Large    1056 / 66    16    6.25%    64 px    16 px    16 px
     * X-Large    1312 / 82    16    6.25%    80 px    16 px    16 px
     * Max    1584 / 99    16    6.25%    96 px    16 px    24 px
     **/
    size?: ScreenSizes;
    /**
     * The Object Map of media sizes and column numbers
     */
    sizes?: { [key in ScreenSizes]: number }

    className?: string;
}

export default function Col({children, offset, span, size = 'lg', className = '', sizes}: IProps) {
    let _className = className;
    if (sizes) {
        Object.keys(sizes).map((media) => {
            _className += ` bx-col-${media}-${sizes[media]}`
        });
    } else {
        if (!span) {
            _className += ` bx--col`;
        } else {
            _className += ` bx--col-${size}-${span}`;
        }
        if (offset) {
            _className += ` bx--offset-${size}-${offset}`
        }
    }

    return (
        <div className={_className}>{children}</div>
    )
}
