import React from "react";
// import PropTypes from 'prop-types';
import { Breadcrumbs as BlueprintBreadcrumbList } from "@blueprintjs/core";
import BreadcrumbItem from './BreadcrumbItem';

function BreadcrumbList ({
    className='',
    itemDivider='/',
    ...otherProps
}: any) {

    const renderBreadcrumb = (propsBreadcrumb) => {
        return <BreadcrumbItem
            {...propsBreadcrumb}
            itemDivider="/"
        />;
    };

    const renderCurrentBreadcrumb = (propsBreadcrumb) => {
        return <BreadcrumbItem
            {...propsBreadcrumb}
            current={true}
            href={null}
            onClick={null}
            itemDivider={itemDivider}
        />;
    };

    return (
        <BlueprintBreadcrumbList
            {...otherProps}
            className={'ecc-breadcrumb__list '+className}
            breadcrumbRenderer={renderBreadcrumb}
            currentBreadcrumbRenderer={renderCurrentBreadcrumb}
        />
    );
};

export default BreadcrumbList;
