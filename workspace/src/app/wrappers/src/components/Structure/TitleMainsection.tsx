import React from "react";

function TitleMainsection ({
    children,
    className = '',
    ...restProps
}: any) {
    let htmlElement = React.createElement('div');
    const childrenArray = React.Children.toArray(children);

    if (
        childrenArray.length === 1 &&
        typeof childrenArray[0] === 'string'
    ) {
        htmlElement = React.createElement('h2');
    }

    return (
        <htmlElement.type
            {...restProps}
            className={'ecc-structure__title-mainsection ' + className}
        >
            {children}
        </htmlElement.type>
    );
}

export default TitleMainsection;
