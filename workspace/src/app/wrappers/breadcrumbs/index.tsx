import React, { memo } from 'react';
import { Breadcrumbs as B_BreadCrumbs, IBreadcrumbProps } from "@blueprintjs/core";

interface IProps {
    paths: IBreadcrumbProps[]
}

const Breadcrumbs = memo(({ paths }: IProps) => {
    const renderCurrentBreadcrumb = ({ text }: IBreadcrumbProps) => {
        // customize rendering of last breadcrumb
        return <span>{text}</span>;
    };

    return (
        <B_BreadCrumbs
            currentBreadcrumbRenderer={renderCurrentBreadcrumb}
            items={paths}
        />
    );
});

export default Breadcrumbs;
