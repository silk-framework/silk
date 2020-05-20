import React, { useState } from "react";
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

interface IPagination {
    // pagination details
    pagination: {
        total: number;
        current: number;
        limit: number;
        minPageSize: number;
    };
    // The pagination component
    paginationElement: any;
    // Should be called whenever the number of total items changes
    onTotalChange: (total: number) => void;
}

interface IPaginationOptions {
    // The initial page size
    initialPageSize?: number;
    // The option of page sizes
    pageSizes?: number[];
    // Presentation options
    presentation?: {
        // For narrow space requirements, the info text in the middle can be hidden
        hideInfoText?: boolean;
    };
}

// Custom hook to add pagination. Currently only use-cases are supported where paging has no further side effects, e.g. REST calls.
export const usePagination = ({
    pageSizes = [5, 10, 25, 50],
    presentation = {},
    initialPageSize,
}: IPaginationOptions): IPagination => {
    const minSize = Math.min(...pageSizes);
    const [pagination, setPagination] = useState({
        total: 0,
        current: 1,
        limit: initialPageSize ? initialPageSize : minSize,
        minPageSize: minSize,
    });
    const onPaginationChange = ({ page, pageSize }) => {
        setPagination({ ...pagination, current: page, limit: pageSize });
    };
    const onTotalChange = (total: number) => {
        setPagination({ ...pagination, total: total, current: 1 });
    };
    const paginationElement = (
        <Pagination
            onChange={onPaginationChange}
            totalItems={pagination.total}
            pageSizes={pageSizes}
            page={pagination.current}
            pageSize={pagination.limit}
            {...presentation}
        />
    );
    return { pagination, paginationElement, onTotalChange };
};

export default Pagination;
