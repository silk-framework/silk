import React, { useEffect, useRef, useState } from "react";
import { IRelatedItem, IRelatedItems } from "@ducks/shared/thunks/relatedItems.thunk";
import {
    Button,
    Card,
    CardActions,
    CardActionsAux,
    CardContent,
    CardHeader,
    CardOptions,
    CardTitle,
    ContextMenu,
    Divider,
    IconButton,
    MenuDivider,
    MenuItem,
    OverviewItem,
    OverviewItemActions,
    OverviewItemDescription,
    OverviewItemLine,
} from "@wrappers/index";
import { sharedOp } from "@ducks/shared";
import DataList from "../Datalist";
import { getItemLinkIcons } from "../SearchList/SearchItem";

export function RelatedItems({ projectId, taskId }) {
    const [loading, setLoading] = useState(true);
    const [data, setData] = useState({ items: [] } as IRelatedItems);

    useEffect(() => {
        getRelatedItemsData(projectId, taskId);
    }, [taskId, projectId]);

    const getRelatedItemsData = async (projectId: string, taskId: string) => {
        setLoading(true);
        const data = await sharedOp.getRelatedItemsAsync(projectId, taskId);
        if (data.items !== undefined) {
            setData(data);
        }
        setLoading(false);
    };

    return (
        <DataList isEmpty={data.items.length === 0} isLoading={loading} hasSpacing hasDivider>
            {data.items.map((relatedItem: IRelatedItem) => {
                const contextMenuItems = relatedItem.itemLinks.map((link) => (
                    <MenuItem key={link.path} text={link.label} href={link.path} icon={getItemLinkIcons(link.label)} />
                ));
                return (
                    <OverviewItem>
                        <OverviewItemDescription>
                            <OverviewItemLine>
                                <span>{relatedItem.label}</span>
                            </OverviewItemLine>
                            <OverviewItemLine small>
                                <span>{relatedItem.type}</span>
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
    );
}
