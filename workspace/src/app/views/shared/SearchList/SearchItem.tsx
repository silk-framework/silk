import React from "react";
import { ISearchResultsServer } from "@ducks/workspace/typings";
import {
    Card,
    ContextMenu,
    Icon,
    IconButton,
    MenuDivider,
    MenuItem,
    OverviewItem,
    OverviewItemActions,
    OverviewItemDepiction,
    OverviewItemDescription,
    OverviewItemLine,
} from "@wrappers/index";
import { routerOp } from "@ducks/router";
import { useDispatch } from "react-redux";
import { Highlighter } from "../Highlighter/Highlighter";
import { getItemLinkIcons } from "../../../utils/getItemLinkIcons";
import { IPageLabels } from "@ducks/router/operations";

interface IProps {
    item: ISearchResultsServer;
    searchValue?: string;

    onOpenDeleteModal(item: ISearchResultsServer);

    onOpenDuplicateModal(item: ISearchResultsServer);

    onRowClick?();
}

export default function SearchItem({ item, searchValue, onOpenDeleteModal, onOpenDuplicateModal, onRowClick }: IProps) {
    const dispatch = useDispatch();

    // Remove detailsPath
    const contextMenuItems = item.itemLinks
        .slice(1)
        .map((link) => (
            <MenuItem
                key={link.path}
                text={link.label}
                href={link.path}
                icon={getItemLinkIcons(link.label)}
                target={"_blank"}
            />
        ));

    const goToDetailsPage = (e) => {
        e.preventDefault();
        const detailsPath = item.itemLinks[0].path;
        const labels: IPageLabels = {};
        if (item.type === "project") {
            labels.projectLabel = item.label;
        } else {
            labels.taskLabel = item.label;
        }

        dispatch(routerOp.goToPage(detailsPath, labels));
    };

    return (
        <Card isOnlyLayout>
            <OverviewItem hasSpacing onClick={onRowClick ? onRowClick : undefined}>
                <OverviewItemDepiction>
                    <Icon name={"artefact-" + item.type} large />
                </OverviewItemDepiction>
                <OverviewItemDescription>
                    <OverviewItemLine>
                        <h4>
                            <Highlighter label={item.label || item.id} searchValue={searchValue} />
                        </h4>
                    </OverviewItemLine>
                    {item.description && (
                        <OverviewItemLine>
                            <p>{item.description}</p>
                        </OverviewItemLine>
                    )}
                </OverviewItemDescription>
                <OverviewItemActions>
                    <IconButton
                        data-test-id={"open-duplicate-modal"}
                        name="item-clone"
                        text="Clone"
                        onClick={onOpenDuplicateModal}
                    />
                    {!!item.itemLinks.length && (
                        <IconButton
                            name="item-viewdetails"
                            text="Show details"
                            onClick={goToDetailsPage}
                            href={item.itemLinks[0].path}
                        />
                    )}
                    <ContextMenu togglerText="Show more options">
                        {contextMenuItems.length ? (
                            <>
                                <MenuDivider />
                                {contextMenuItems}
                            </>
                        ) : null}
                        <MenuItem key="delete" icon={"item-remove"} onClick={onOpenDeleteModal} text={"Delete"} />
                    </ContextMenu>
                </OverviewItemActions>
            </OverviewItem>
        </Card>
    );
}
