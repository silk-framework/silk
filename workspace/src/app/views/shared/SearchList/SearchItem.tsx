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
    OverflowText,
    Spacing,
    Tag,
} from "@wrappers/index";
import { routerOp } from "@ducks/router";
import { useDispatch, useSelector } from "react-redux";
import { Highlighter } from "../Highlighter/Highlighter";
import { ResourceLink } from "../ResourceLink/ResourceLink";
import { getItemLinkIcons } from "../../../utils/getItemLinkIcons";
import { IPageLabels } from "@ducks/router/operations";
import { DATA_TYPES } from "../../../constants";
import { commonSel } from "@ducks/common";
import { IExportTypes } from "@ducks/common/typings";
import { downloadResource } from "../../../utils/downloadResource";

interface IProps {
    item: ISearchResultsServer;
    searchValue?: string;

    onOpenDeleteModal(item: ISearchResultsServer);

    onOpenDuplicateModal(item: ISearchResultsServer);

    onRowClick?();

    parentProjectId?: string;
}

export default function SearchItem({
    item,
    searchValue,
    onOpenDeleteModal,
    onOpenDuplicateModal,
    onRowClick,
    parentProjectId,
}: IProps) {
    const dispatch = useDispatch();
    const exportTypes = useSelector(commonSel.exportTypesSelector);

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
        labels.itemType = item.type;
        dispatch(routerOp.goToPage(detailsPath, labels));
    };

    const handleExport = async (type: IExportTypes) => {
        downloadResource(item.id, type.id);
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
                            <ResourceLink
                                url={!!item.itemLinks.length ? item.itemLinks[0].path : false}
                                handlerResourcePageLoader={!!item.itemLinks.length ? goToDetailsPage : false}
                            >
                                <Highlighter label={item.label || item.id} searchValue={searchValue} />
                            </ResourceLink>
                        </h4>
                    </OverviewItemLine>
                    {(item.description || item.projectId) && (
                        <OverviewItemLine small>
                            <OverflowText>
                                {!parentProjectId && item.type !== "project" && (
                                    <Tag>{item.projectLabel ? item.projectLabel : item.projectId}</Tag>
                                )}
                                {item.description && !parentProjectId && item.type !== "project" && (
                                    <Spacing vertical size="small" />
                                )}
                                {item.description && <Highlighter label={item.description} searchValue={searchValue} />}
                            </OverflowText>
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
                                {contextMenuItems}
                                <MenuDivider />
                            </>
                        ) : null}
                        <MenuItem key="delete" icon={"item-remove"} onClick={onOpenDeleteModal} text={"Delete"} />
                        {item.type === DATA_TYPES.PROJECT && !!exportTypes.length && (
                            <MenuItem key="export" text={"Export to"}>
                                {exportTypes.map((type) => (
                                    <MenuItem key={type.id} onClick={() => handleExport(type)} text={type.label} />
                                ))}
                            </MenuItem>
                        )}
                    </ContextMenu>
                </OverviewItemActions>
            </OverviewItem>
        </Card>
    );
}
