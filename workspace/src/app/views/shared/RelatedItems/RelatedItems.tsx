import React, { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import { Card, CardContent, CardHeader, CardTitle, Divider } from "@eccenca/gui-elements";
import DataList from "../Datalist";
import Spacing from "@eccenca/gui-elements/src/components/Separation/Spacing";
import { IRelatedItem, IRelatedItemsResponse } from "@ducks/shared/typings";
import { commonSel } from "@ducks/common";
import { SearchBar } from "../SearchBar/SearchBar";
import { usePagination } from "@eccenca/gui-elements/src/components/Pagination/Pagination";
import { useTranslation } from "react-i18next";
import { RelatedItem } from "./RelatedItem";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { requestRelatedItems } from "@ducks/shared/requests";

interface IProps {
    projectId?: string;
    taskId?: string;
    // If defined, receives the message ID of the message event. Returns true if the related items should be reloaded.
    messageEventReloadTrigger?: (messageId: string, message: string) => boolean;
}

/** Widget that shows related items of project tasks*/
export function RelatedItems(props: IProps) {
    const _projectId = useSelector(commonSel.currentProjectIdSelector);
    const _taskId = useSelector(commonSel.currentTaskIdSelector);
    const { isOpen } = useSelector(commonSel.artefactModalSelector);
    const { registerError } = useErrorHandler();

    const projectId = props.projectId || _projectId;
    const taskId = props.taskId || _taskId;

    const [loading, setLoading] = useState(true);
    const [data, setData] = useState({ total: 0, items: [] } as IRelatedItemsResponse);
    const [textQuery, setTextQuery] = useState("");
    const [updated, setUpdated] = useState(0);
    const [pagination, paginationElement, onTotalChange] = usePagination({
        initialPageSize: 5,
        pageSizes: [5, 10, 20],
        presentation: { hideInfoText: true },
    });
    // If an I-Frame sends an event to update the page, decide based on provided function if to reload.
    useEffect(() => {
        if (props.messageEventReloadTrigger) {
            let updateSwitchValue = updated;
            const handler = (event) => {
                try {
                    const data = JSON.parse(event.data);
                    if (typeof data?.id === "string") {
                        if (props.messageEventReloadTrigger?.(data.id, data.message)) {
                            updateSwitchValue = 1 - updateSwitchValue;
                            setUpdated(updateSwitchValue);
                        }
                    }
                } catch (ex) {}
            };
            // Add message event listener
            window.addEventListener("message", handler);

            // clean up
            return () => window.removeEventListener("message", handler);
        }
    }, []);

    const [t] = useTranslation();

    useEffect(() => {
        if (projectId && taskId && !isOpen) {
            getRelatedItemsData(projectId, taskId, textQuery);
        }
    }, [projectId, taskId, textQuery, updated, isOpen]);

    // Fetches and updates the related items of the project task
    const getRelatedItemsData = async (projectId: string, taskId: string, textQuery: string) => {
        setLoading(true);
        try {
            const response = await requestRelatedItems(projectId, taskId, textQuery);
            if (response.data.items) {
                onTotalChange(response.data.total);
                setData(response.data);
            }
        } catch (ex) {
            registerError("RelatedItems-getRelatedItemsData", "Failed to fetch related items.", ex);
        } finally {
            setLoading(false);
        }
    };

    // Postfix for the title showing the filtered number and total number of related items.
    const relatedItemsSizeInfo = (length: number, total: number) => {
        if (total > 0) {
            if (length === total) {
                return ` (${total})`; // Don't repeat if they are the same
            } else {
                return ` (${length} / ${total})`;
            }
        } else {
            return ""; // Don't show anything if there is no related item at all.
        }
    };

    const searchFired = (searchInput: string) => {
        setTextQuery(searchInput);
    };

    return (
        <Card data-test-id={"related-items-widget"}>
            <CardHeader>
                <CardTitle>
                    <h2>
                        {t("RelatedItems.title", "Related items")}
                        {relatedItemsSizeInfo(data.items.length, data.total)}
                    </h2>
                </CardTitle>
            </CardHeader>
            <Divider />
            <CardContent style={{ maxHeight: "25vh" }}>
                {(data.total > 0 || textQuery !== "") && <SearchBar textQuery={textQuery} onSearch={searchFired} />}
                <Spacing size="small" />
                <DataList
                    isEmpty={data.items.length === 0}
                    isLoading={loading}
                    emptyListMessage={t("common.messages.noItems", { items: "items" })}
                    hasSpacing
                    hasDivider
                >
                    {data.items
                        .slice((pagination.current - 1) * pagination.limit, pagination.current * pagination.limit)
                        .map((relatedItem: IRelatedItem) => (
                            <RelatedItem key={relatedItem.id} relatedItem={relatedItem} textQuery={textQuery} />
                        ))}
                </DataList>
                {data.items.length > Math.min(pagination.total, 5) ? ( // Don't show if no pagination is needed
                    <>
                        <Spacing size="small" />
                        {paginationElement}
                    </>
                ) : null}
            </CardContent>
        </Card>
    );
}
