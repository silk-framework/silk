import React from "react";
import { ISearchResultsServer } from "@ducks/workspace/typings";
import { ContextMenu, IconButton, MenuDivider, MenuItem, OverflowText } from "@eccenca/gui-elements";
import { routerOp } from "@ducks/router";
import { useDispatch, useSelector } from "react-redux";
import { getItemLinkIcons } from "../../../utils/getItemLinkIcons";
import { DATA_TYPES } from "../../../constants";
import { commonSel } from "@ducks/common";
import { IExportTypes } from "@ducks/common/typings";
import { downloadProject } from "../../../utils/downloadProject";
import { useTranslation } from "react-i18next";
import { useProjectTaskTabsView } from "../projectTaskTabView/projectTaskTabsViewHooks";
import { AppDispatch } from "store/configureStore";
import { useItemNavigation } from "./useItemNavigation";

export interface SearchItemActionsProps {
    item: ISearchResultsServer;

    onOpenDeleteModal(item: ISearchResultsServer);

    onOpenDuplicateModal(item: ISearchResultsServer);

    onOpenCopyToModal(item: ISearchResultsServer);

    toggleShowIdentifierModal(item: ISearchResultsServer);

    /** When true, render only the "more options" menu (no inline clone/details icon buttons). */
    compact?: boolean;
}

/**
 * The action controls of a single search result item: the inline "clone"/"show details" icon
 * buttons and the "more options" context menu. Shared by every presentation of an item (row card,
 * table row, grid card) so the available actions stay identical across views.
 */
export default function SearchItemActions({
    item,
    onOpenDeleteModal,
    onOpenDuplicateModal,
    onOpenCopyToModal,
    toggleShowIdentifierModal,
    compact = false,
}: SearchItemActionsProps) {
    const dispatch = useDispatch<AppDispatch>();
    const exportTypes = useSelector(commonSel.exportTypesSelector);
    const [t] = useTranslation();
    const { itemLinks, detailsPath, goToDetailsPage } = useItemNavigation(item);
    // Remove detailsPath
    const menuItemLinks = itemLinks.slice(1);
    const { projectTabView, changeTab, menuItems } = useProjectTaskTabsView({
        srcLinks: menuItemLinks.map((link) => ({ ...link, id: link.label })),
        pluginId: item.pluginId,
        projectId: item.projectId,
        taskId: item.id,
    });

    const handleExport = async (type: IExportTypes) => {
        downloadProject(item.id, type.id);
    };

    const contextMenuItems = [
        ...menuItems,
        ...menuItemLinks.map((link) => (
            <MenuItem
                key={link.path}
                text={t("common.legacyGui." + link.label, link.label)}
                icon={getItemLinkIcons(link.label)}
                onClick={() =>
                    changeTab({
                        id: link.label,
                        path: link.path,
                        label: link.label,
                        itemType: undefined,
                    })
                }
            />
        )),
    ];

    if (item.type === DATA_TYPES.PROJECT && !!exportTypes.length) {
        contextMenuItems.push(
            <MenuItem key="export" text={t("common.action.export", "Export to")}>
                {exportTypes.map((type) => (
                    <MenuItem
                        key={type.id}
                        onClick={() => handleExport(type)}
                        text={<OverflowText inline>{type.label}</OverflowText>}
                    />
                ))}
            </MenuItem>,
        );
    }

    return (
        <>
            {!compact && (
                <IconButton
                    data-test-id={"open-duplicate-modal"}
                    name="item-clone"
                    text={t("common.action.clone", "Clone")}
                    onClick={() => onOpenDuplicateModal(item)}
                />
            )}
            {!compact && !!itemLinks.length && (
                <IconButton
                    name="item-viewdetails"
                    text={t("common.action.showDetails", "Show details")}
                    onClick={goToDetailsPage}
                    href={detailsPath}
                />
            )}
            <ContextMenu
                data-test-id={"search-item-context-menu"}
                togglerText={t("common.action.moreOptions", "Show more options")}
            >
                {contextMenuItems.length ? (
                    <>
                        {contextMenuItems}
                        <MenuDivider />
                    </>
                ) : (
                    <></>
                )}
                <MenuItem
                    data-test-id="search-item-copy-btn"
                    key="copy"
                    icon="item-copy"
                    onClick={() => onOpenCopyToModal(item)}
                    text={t("common.action.copy", "Copy")}
                />
                {itemLinks.length ? (
                    <MenuItem
                        icon="item-viewdetails"
                        text={t("common.action.showDetails", "Show details")}
                        key="view"
                        onClick={goToDetailsPage}
                        href={detailsPath}
                    />
                ) : (
                    <></>
                )}
                <MenuItem
                    data-test-id={"open-duplicate-modal"}
                    icon="item-clone"
                    text={t("common.action.clone", "Clone")}
                    onClick={() => onOpenDuplicateModal(item)}
                />
                <MenuItem
                    data-test-id={"open-duplicate-modal"}
                    icon="item-viewdetails"
                    text={t("common.action.showIdentifier", "Show identifier")}
                    onClick={() => toggleShowIdentifierModal(item)}
                />
                {item.type === DATA_TYPES.PROJECT ? (
                    <MenuItem
                        data-test-id={"search-item-activities-btn"}
                        icon="application-activities"
                        text={t("widget.ActivityInfoWidget.title", "Activities")}
                        onClick={(e) => {
                            e.preventDefault();
                            e.stopPropagation();
                            dispatch(
                                routerOp.goToPage(
                                    `projects/${item.id}/activities?page=1&limit=25&sortBy=recentlyUpdated&sortOrder=ASC`,
                                ),
                            );
                        }}
                    />
                ) : (
                    <></>
                )}
                <MenuDivider />
                <MenuItem
                    data-test-id="search-item-delete-btn"
                    key="delete"
                    icon={"item-remove"}
                    onClick={() => onOpenDeleteModal(item)}
                    text={t("common.action.delete", "Delete")}
                    intent="danger"
                />
            </ContextMenu>
            {projectTabView}
        </>
    );
}
