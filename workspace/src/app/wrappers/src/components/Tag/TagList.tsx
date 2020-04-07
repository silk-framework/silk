import React from 'react';

function TagList({
    children,
    className = '',
    label = '',
    ...otherProps
}: any) {



    const tagList = (
        <ul
            className={
                'ecc-tag__list' +
                ((className && label) ? ' ' + className : '')
            }
        >
            {
                React.Children.map(children, (child, i) => {
                    return <li className={'ecc-tag__list-item'} key={'tagitem_'+i}>
                        {child}
                    </li>
                })
            }
        </ul>
    );

    if (label) {
        return (
            <div
                className={
                    'ecc-tag__list-wrapper' +
                    (className ? ' ' + className : '')
                }
            >
                <strong className={'ecc-tag__list-label'}>
                    {label}
                </strong>
                <span className={'ecc-tag__list-content'}>
                    {tagList}
                </span>
            </div>
        );
    }

    return tagList;
};

export default TagList;
