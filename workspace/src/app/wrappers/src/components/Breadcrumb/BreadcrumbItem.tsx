import React from "react";
// import PropTypes from 'prop-types';
import {
    Breadcrumb as BlueprintBreadcrumbItem,
    IBreadcrumbProps as IBlueprintBreadcrumbItemProps,
} from "@blueprintjs/core";

export interface IBreadcrumbItemProps extends IBlueprintBreadcrumbItemProps {
    intent?: never;
}

function BreadcrumbItem({
    className = "",
    //itemDivider='',
    ...otherProps
}: IBreadcrumbItemProps) {
    /*
        TODO: adding `data-divider` does not work this way because BlueprintJS
        breadcrumb component does not support (and forward) it on HTML element
        level. The idea is to add the divider as data-* property to use it via
        CSS/Sass as content for the pseudo element, currently done static in CSS
        with slash char.
    */
    return (
        <BlueprintBreadcrumbItem
            {...otherProps}
            className={"ecc-breadcrumb__item " + className}
            /* data-divider={itemDivider ? itemDivider : ''} */
        />
    );
}

export default BreadcrumbItem;
