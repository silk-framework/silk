import React, { memo } from 'react';
import { Breadcrumbs as B_BreadCrumbs, IBreadcrumbProps } from "@blueprintjs/core";
import styles from "./index.module.scss";

interface IProps {
    paths: IBreadcrumbProps[]
}

const Breadcrumbs = memo(({ paths }: IProps) => {
    return (
            <B_BreadCrumbs
                className={styles.customBread}
                items={paths}
            />
    );
});

export default Breadcrumbs;
