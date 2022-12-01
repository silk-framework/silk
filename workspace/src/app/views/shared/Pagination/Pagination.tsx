import { Link, Pagination, Spacing } from "@eccenca/gui-elements";
import React from "react";
import { useTranslation } from "react-i18next";

export function AppPagination({ pagination, onChangeSelect, pageSizes }) {
    const [t] = useTranslation();

    const totalGreaterThanMinPageSize = pagination.total > Math.min(pagination.limit, ...pageSizes);
    const invalidPage = pagination.current !== 1 && Math.ceil(pagination.total / pagination.limit) < pagination.current;

    if (invalidPage) {
        return (
            <div>
                <Spacing size={"tiny"} />
                {t("Pagination.invalidPageError")}
                <Link href={"#"} onClick={() => onChangeSelect(1, pagination.limit)}>
                    {t("Pagination.backToPage1")}
                </Link>
                .
            </div>
        );
    } else if (totalGreaterThanMinPageSize) {
        return (
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
        );
    } else {
        return null;
    }
}
