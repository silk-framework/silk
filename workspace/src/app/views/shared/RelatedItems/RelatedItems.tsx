import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import {
    IconButton,
    Card,
    CardContent,
    CardHeader,
    CardTitle,
    ContextMenu,
    Divider,
    MenuItem,
    OverviewItem,
    OverviewItemActions,
    OverviewItemDescription,
    OverviewItemLine,
} from "@wrappers/index";
import { sharedOp } from "@ducks/shared";
import { routerOp } from "@ducks/router";
import DataList from "../Datalist";
import Tag from "@wrappers/src/components/Tag/Tag";
import { getItemLinkIcons } from "../../../utils/getItemLinkIcons";
import Spacing from "@wrappers/src/components/Separation/Spacing";
import { Highlighter } from "../Highlighter/Highlighter";
import { ResourceLink } from "../ResourceLink/ResourceLink";
import { IItemLink, IRelatedItem, IRelatedItemsResponse } from "@ducks/shared/typings";
import { commonSel } from "@ducks/common";
import { SearchBar } from "../SearchBar/SearchBar";
import { usePagination } from "@wrappers/src/components/Pagination/Pagination";

interface IProps {
    projectId?: string;
    taskId?: string;
}

/** Widget that shows related items of project tasks*/
export function RelatedItems(props: IProps) {
    const _projectId = useSelector(commonSel.currentProjectIdSelector);
    const _taskId = useSelector(commonSel.currentTaskIdSelector);

    const projectId = props.projectId || _projectId;
    const taskId = props.taskId || _taskId;

    const [loading, setLoading] = useState(true);
    const [data, setData] = useState({ total: 0, items: [] } as IRelatedItemsResponse);
    const [textQuery, setTextQuery] = useState("");
    const { pagination, paginationElement, onTotalChange } = usePagination({
        initialPageSize: 5,
        pageSizes: [5, 10, 20],
        presentation: { hideInfoText: true },
    });
    const dispatch = useDispatch();

    useEffect(() => {
        if (projectId) {
            getRelatedItemsData(projectId, taskId, textQuery);
        }
    }, [taskId, projectId, textQuery]);

    // Fetches and updates the related items of the project task
    const getRelatedItemsData = async (projectId: string, taskId: string, textQuery: string) => {
        setLoading(true);
        const data = await sharedOp.getRelatedItemsAsync(projectId, taskId, textQuery);
        if (data.items) {
            onTotalChange(data.total);
            setData(data);
        }
        setLoading(false);
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

    const goToDetailsPage = (resourceItem: IItemLink, taskLabel: string, event) => {
        event.preventDefault();
        dispatch(routerOp.goToPage(resourceItem.path, { taskLabel }));
    };

    return (
        <Card>
            <CardHeader>
                <CardTitle>
                    <h2>Related items{relatedItemsSizeInfo(data.items.length, data.total)}</h2>
                </CardTitle>
            </CardHeader>
            <Divider />
            <CardContent>
                {data.total > 0 || textQuery !== "" ? (
                    <SearchBar textQuery={textQuery} onSearch={searchFired} />
                ) : (
                    false
                )}
                <Spacing size="small" />
                <DataList
                    isEmpty={data.items.length === 0}
                    isLoading={loading}
                    emptyListMessage={"No items found."}
                    hasSpacing
                    hasDivider
                >
                    {data.items
                        .slice((pagination.current - 1) * pagination.limit, pagination.current * pagination.limit)
                        .map((relatedItem: IRelatedItem) => {
                            const contextMenuItems = relatedItem.itemLinks.map((link, idx) => (
                                <MenuItem
                                    key={link.path}
                                    text={link.label}
                                    href={link.path}
                                    icon={getItemLinkIcons(link.label)}
                                    onClick={
                                        idx === 0
                                            ? (e) => goToDetailsPage(relatedItem.itemLinks[0], relatedItem.label, e)
                                            : null
                                    }
                                />
                            ));
                            return (
                                <OverviewItem key={relatedItem.id} densityHigh>
                                    <OverviewItemDescription>
                                        <OverviewItemLine>
                                            <span>
                                                <Tag>
                                                    <Highlighter label={relatedItem.type} searchValue={textQuery} />
                                                </Tag>{" "}
                                                <ResourceLink
                                                    url={
                                                        !!relatedItem.itemLinks.length
                                                            ? relatedItem.itemLinks[0].path
                                                            : false
                                                    }
                                                    handlerResourcePageLoader={
                                                        !!relatedItem.itemLinks.length
                                                            ? (e) =>
                                                                  goToDetailsPage(
                                                                      relatedItem.itemLinks[0],
                                                                      relatedItem.label,
                                                                      e
                                                                  )
                                                            : false
                                                    }
                                                >
                                                    <Highlighter label={relatedItem.label} searchValue={textQuery} />
                                                </ResourceLink>
                                            </span>
                                        </OverviewItemLine>
                                    </OverviewItemDescription>
                                    <OverviewItemActions>
                                        {!!relatedItem.itemLinks.length && (
                                            <IconButton
                                                name="item-viewdetails"
                                                text="Show details"
                                                onClick={(e) =>
                                                    goToDetailsPage(relatedItem.itemLinks[0], relatedItem.label, e)
                                                }
                                                href={relatedItem.itemLinks[0].path}
                                            />
                                        )}
                                        {contextMenuItems.length && (
                                            <ContextMenu togglerText="Show more options">
                                                {contextMenuItems}
                                            </ContextMenu>
                                        )}
                                    </OverviewItemActions>
                                </OverviewItem>
                            );
                        })}
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
