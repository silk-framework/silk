import React, { memo } from 'react';
import { Breadcrumbs as B_BreadCrumbs, IBreadcrumbProps } from "@blueprintjs/core";
import styles from "./index.module.scss";

interface IProps {
    paths: IBreadcrumbProps[]
}

const Breadcrumbs = memo(({ paths }: IProps) => {
    // const renderCurrentBreadcrumb = ({ path, label }) => {
    //     // customize rendering of last breadcrumb
    //     return <span>{text}</span>;
    // };
    return (
        <div className={styles.customBread}>
            <B_BreadCrumbs
                className={styles.customBread}
                items={paths}
            />
        </div>
    );
});

export default Breadcrumbs;
