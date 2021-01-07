import React from "react";
import { ISearchResultsServer } from "@ducks/workspace/typings";
import {
    Card,
    ContextMenu,
    Highlighter,
    IconButton,
    MenuDivider,
    MenuItem,
    OverflowText,
    OverviewItem,
    OverviewItemActions,
    OverviewItemDepiction,
    OverviewItemDescription,
    OverviewItemLine,
    Spacing,
    Tag,
} from "@gui-elements/index";
import { routerOp } from "@ducks/router";
import { useDispatch, useSelector } from "react-redux";
import { ResourceLink } from "../ResourceLink/ResourceLink";
import { getItemLinkIcons } from "../../../utils/getItemLinkIcons";
import { IPageLabels } from "@ducks/router/operations";
import { DATA_TYPES } from "../../../constants";
import { commonSel } from "@ducks/common";
import { IExportTypes } from "@ducks/common/typings";
import { downloadResource } from "../../../utils/downloadResource";
import { useTranslation } from "react-i18next";
import ItemDepiction from "../../shared/ItemDepiction";
import { useIFrameWindowLinks } from "../IframeWindow/iframewindowHooks";

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
    const [t] = useTranslation();
    // Remove detailsPath
    const itemLinks = item.itemLinks.slice(1);
    const { iframeWindow, toggleIFrameLink } = useIFrameWindowLinks(itemLinks);
    const contextMenuItems = itemLinks.map((link) => (
        <MenuItem
            key={link.path}
            text={t("common.legacyGui." + link.label, link.label)}
            icon={getItemLinkIcons(link.label)}
            onClick={() =>
                toggleIFrameLink({
                    path: link.path,
                    label: link.label,
                    itemType: null,
                })
            }
        />
    ));

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
            </MenuItem>
        );
    }

    const goToDetailsPage = (e) => {
        // Only open page in same tab if user did not try to open in new tab
        if (!e?.ctrlKey) {
            e.preventDefault();
            const detailsPath = item.itemLinks[0].path;
            const labels: IPageLabels = {};
            if (item.type === DATA_TYPES.PROJECT) {
                labels.projectLabel = item.label;
            } else {
                labels.taskLabel = item.label;
            }
            labels.itemType = item.type;
            dispatch(routerOp.goToPage(detailsPath, labels));
        }
    };

    const handleExport = async (type: IExportTypes) => {
        downloadResource(item.id, type.id);
    };

    return (
        <Card isOnlyLayout>
            <OverviewItem hasSpacing onClick={onRowClick ? onRowClick : undefined}>
                <OverviewItemDepiction>
                    <ItemDepiction itemType={item.type} pluginId={item.pluginId} />
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
                                {!parentProjectId && item.type !== DATA_TYPES.PROJECT && (
                                    <Tag>
                                        <Highlighter
                                            label={item.projectLabel ? item.projectLabel : item.projectId}
                                            searchValue={searchValue}
                                        />
                                    </Tag>
                                )}
                                {item.description && !parentProjectId && item.type !== DATA_TYPES.PROJECT && (
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
                        text={t("common.action.clone", "Clone")}
                        onClick={onOpenDuplicateModal}
                    />
                    {!!item.itemLinks.length && (
                        <IconButton
                            name="item-viewdetails"
                            text={t("common.action.showDetails", "Show details")}
                            onClick={goToDetailsPage}
                            href={item.itemLinks[0].path}
                        />
                    )}
                    <ContextMenu togglerText={t("common.action.moreOptions", "Show more options")}>
                        {contextMenuItems.length ? (
                            <>
                                {contextMenuItems}
                                <MenuDivider />
                            </>
                        ) : null}
                        <MenuItem
                            data-test-id="search-item-delete-btn"
                            key="delete"
                            icon={"item-remove"}
                            onClick={onOpenDeleteModal}
                            text={t("common.action.delete", "Delete")}
                        />
                    </ContextMenu>
                </OverviewItemActions>
            </OverviewItem>
            {iframeWindow}
        </Card>
    );
}
