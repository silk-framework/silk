import React from 'react';
import PaginationC from "@wrappers/carbon/Pagination"

export default function Pagination({pagination, onChangeSelect, pageSizes}) {
    return (
        <PaginationC
            onChange={({page, pageSize}) => {onChangeSelect(page, pageSize)} }
            totalItems={pagination.total}
            pageSizes={pageSizes}
            page={pagination.current}
            pageSize={pagination.limit}
        >
        </PaginationC>
    );
}