import React, { memo } from 'react';
import { Breadcrumbs as B_BreadCrumbs, IBreadcrumbProps } from "@blueprintjs/core";

interface IProps {
    paths: IBreadcrumbProps[]
}

const Breadcrumbs = memo(({ paths }: IProps) => {
    // const renderCurrentBreadcrumb = ({ path, label }) => {
    //     // customize rendering of last breadcrumb
    //     return <span>{text}</span>;
    // };
    return (
        <B_BreadCrumbs
            items={paths}
        />
    );
});

export default Breadcrumbs;
