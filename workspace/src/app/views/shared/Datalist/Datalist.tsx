import React from "react";
import Spinner from "@wrappers/blueprint/spinner";
import { OverviewItemList } from '@wrappers/index'

const _loadingIndicator = () => <Spinner/>;

const _emptyContent = () => <p>No resources found</p>;

export function Datalist({children, isLoading = false, data, ...otherProps}) {
    if (isLoading) {
        return _loadingIndicator();
    } else if (!data.length) {
        return _emptyContent();
    }
    return (
            <OverviewItemList {...otherProps}>
                {children}
            </OverviewItemList>
    )
}
