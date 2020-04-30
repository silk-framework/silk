import React, { useEffect, useState } from "react";
import { getRelatedItemsAsync, IRelatedItem, IRelatedItems } from "@ducks/shared/thunks/relatedItems.thunk";
import {
    Card,
    CardContent,
    CardHeader,
    CardTitle,
    ContextMenu,
    Divider,
    MenuDivider,
    MenuItem,
    OverviewItem,
    OverviewItemActions,
    OverviewItemDescription,
    OverviewItemLine,
    Spacing,
} from "@wrappers/index";
import { sharedOp } from "@ducks/shared";
import DataList from "../Datalist";
import { getItemLinkIcons } from "../SearchList/SearchItem";
import { RelatedItemsSearch } from "./RelatedItemsSearch";
import Tag from "@wrappers/src/components/Tag/Tag";

export function RelatedItems({ projectId, taskId }) {
    const [loading, setLoading] = useState(true);
    const [data, setData] = useState({ items: [] } as IRelatedItems);
    const [textQuery, setTextQuery] = useState("");

    useEffect(() => {
        getRelatedItemsData(projectId, taskId, textQuery);
    }, [taskId, projectId, textQuery]);

    const getRelatedItemsData = async (projectId: string, taskId: string, textQuery: string) => {
        setLoading(true);
        const data = await sharedOp.getRelatedItemsAsync(projectId, taskId, textQuery);
        if (data.items !== undefined) {
            setData(data);
        }
        setLoading(false);
    };

    const relatedItemsSizeInfo = (length: number) => {
        if (length > 0) {
            return ` (${length})`;
        } else {
            return "";
        }
    };

    const searchFired = (searchInput: string) => {
        setTextQuery(searchInput);
    };

    return (
        <Card>
            <CardHeader>
                <CardTitle>
                    <h3>Related Items{relatedItemsSizeInfo(data.items.length)}</h3>
                </CardTitle>
            </CardHeader>
            <Divider />
            <CardContent>
                {data.items.length > 0 || textQuery != "" ? <RelatedItemsSearch onSearch={searchFired} /> : false}
                <Spacing size="small" />
                <DataList isEmpty={data.items.length === 0} isLoading={loading} hasSpacing hasDivider>
                    {data.items.map((relatedItem: IRelatedItem) => {
                        const contextMenuItems = relatedItem.itemLinks.map((link) => (
                            <MenuItem
                                key={link.path}
                                text={link.label}
                                href={link.path}
                                icon={getItemLinkIcons(link.label)}
                            />
                        ));
                        return (
                            <OverviewItem densityHigh>
                                <OverviewItemDescription>
                                    <OverviewItemLine>
                                        <span>
                                            <Tag>{relatedItem.type}</Tag> {relatedItem.label}
                                        </span>
                                    </OverviewItemLine>
                                </OverviewItemDescription>
                                <OverviewItemActions>
                                    <ContextMenu togglerText="Show more options">
                                        {contextMenuItems.length ? (
                                            <>
                                                <MenuDivider />
                                                {contextMenuItems}
                                            </>
                                        ) : null}
                                    </ContextMenu>
                                </OverviewItemActions>
                            </OverviewItem>
                        );
                    })}
                </DataList>
            </CardContent>
        </Card>
    );
}
