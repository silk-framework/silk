import React from "react";
import { Pagination as CarbonPagination } from "carbon-components-react";

function Pagination({
    className,
    hidePageSizeConfiguration = false,
    hideInfoText = false,
    hidePageSelect = false,
    hideNavigationArrows = false,
    ...otherProps
}: any) {
    return (
        <CarbonPagination
            {...otherProps}
            className={
                "ecc-pagination" +
                (className ? " " + className : "") +
                (hidePageSizeConfiguration ? " ecc-pagination--hidepagesize" : "") +
                (hideInfoText ? " ecc-pagination--hideinfotext" : "") +
                (hidePageSelect ? " ecc-pagination--hidepageselect" : "") +
                (hideNavigationArrows ? " ecc-pagination--hidenavigation" : "")
            }
        />
    );
}

export default Pagination;
