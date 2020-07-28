import React from "react";
import { Pagination } from "@wrappers/index";
import { useTranslation } from "react-i18next";

// TODO: why do we not use the Pagination element directly, don't see any real benefit of this wrapper here
export function AppPagination({ pagination, onChangeSelect, pageSizes }) {
    const [t] = useTranslation();

    return pagination.total > Math.min(pagination.limit, ...pageSizes) ? ( // always allow to change to other page sizes
        <Pagination
            onChange={({ page, pageSize }) => {
                onChangeSelect(page, pageSize);
            }}
            totalItems={pagination.total}
            pageSizes={pageSizes}
            page={pagination.current}
            pageSize={pagination.limit}
            backwardText={t("Pagination.backwardText", "Previous page")}
            forwardText={t("Pagination.forwardText", "Next page")}
            itemsPerPageText={t("Pagination.itemsPerPage", "Items per page:")}
        />
    ) : null;
}
