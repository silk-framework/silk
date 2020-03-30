import React from 'react';
import { Card as BlueprintCard } from "@blueprintjs/core";

function Card({
    children,
    className='',
    elevation=1,
    isOnlyLayout=false,
    ...otherProps
}: any) {
    const cardElement = (
        <BlueprintCard
            {...otherProps}
            elevation={elevation}
            className={'ecc-card ' + className}
        >
            {children}
        </BlueprintCard>
    );

    // TODO: improve Card element so it is itself a section html element
    return isOnlyLayout === false ? <section>{cardElement}</section> : cardElement;
};

export default Card;
