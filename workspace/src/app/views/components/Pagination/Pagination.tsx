import React from 'react';
import CPagination from "@wrappers/carbon/Pagination"

export function Pagination({pagination, onChangeSelect, pageSizes}) {
    return (
        <CPagination
            onChange={({page, pageSize}) => {onChangeSelect(page, pageSize)} }
            totalItems={pagination.total}
            pageSizes={pageSizes}
            page={pagination.current}
            pageSize={pagination.limit}
        />
    );
}
