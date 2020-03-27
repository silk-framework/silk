import React from "react";
// import PropTypes from 'prop-types';
import { Breadcrumb as BlueprintBreadcrumbItem } from "@blueprintjs/core";

function BreadcrumbItem ({
    className='',
    itemDivider='',
    ...otherProps
}: any) {

    /*
        TODO: adding `data-divider` does not work this way because BlueprintJS
        breadcrumb component does not support (and forward) it on HTML element
        level. The idea is to add the divider as data-* property to use it via
        CSS/Sass as content for the pseudo element, currently done static with
        slash char.
    */
    return (
        <BlueprintBreadcrumbItem
            {...otherProps}
            className={'ecc-breadcrumb__item '+className}
            data-divider={itemDivider ? itemDivider : ''}
        />
    );
};

export default BreadcrumbItem;
