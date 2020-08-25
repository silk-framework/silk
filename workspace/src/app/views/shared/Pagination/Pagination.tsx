import React from "react";
import { Pagination } from "@gui-elements/index";
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
            itemRangeText={(min, max, total) => t("Pagination.itemRangeText", { min: min, max: max, total: total })}
            pageRangeText={(current, total) => t("Pagination.pageRangeText", { total })}
        />
    ) : null;
}
