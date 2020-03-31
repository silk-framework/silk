import React from "react";

function TitleSubsection ({
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
        htmlElement = React.createElement('h3');
    }

    return (
        <htmlElement.type
            {...restProps}
            className={'ecc-structure__title-subsection ' + className}
        >
            {children}
        </htmlElement.type>
    );
}

export default TitleSubsection;
