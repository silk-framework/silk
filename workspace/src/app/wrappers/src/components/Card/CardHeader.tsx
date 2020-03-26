import React from 'react';
import {
    OverviewItem,
    OverviewItemDescription,
}from './../OverviewItem';
import CardTitle from './CardTitle';
import CardOptions from './CardOptions';

const CardHeader = ({
    children,
    className='',
    densityHigh = true,
    ...otherProps
}: any) => {
    let actions = [];
    let description = [];

    React.Children.map(children, (child, i) => {
        switch (child.type) {
            case CardTitle:
                description.push(child);
                break;
            case CardOptions:
                actions.push(child);
                break;
        }
    })

    return (
        <header>
            <OverviewItem
                {...otherProps}
                className={'ecc-card__header ' + className}
                densityHigh={densityHigh}
            >
                {description.length > 0 && <OverviewItemDescription>{description}</OverviewItemDescription>}
                {actions}
            </OverviewItem>
        </header>
    );
};

export default CardHeader;
