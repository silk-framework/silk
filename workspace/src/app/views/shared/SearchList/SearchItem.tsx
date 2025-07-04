import React from "react";
import { ISearchResultsServer } from "@ducks/workspace/typings";
import {
    Card,
    ContextMenu,
    Highlighter,
    Icon,
    IconButton,
    Markdown,
    MarkdownProps,
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
    TagList,
    markdownUtils,
} from "@eccenca/gui-elements";
import { routerOp } from "@ducks/router";
import { useDispatch, useSelector } from "react-redux";
import { ResourceLink } from "../ResourceLink/ResourceLink";
import { getItemLinkIcons } from "../../../utils/getItemLinkIcons";
import { IPageLabels } from "@ducks/router/operations";
import { DATA_TYPES } from "../../../constants";
import { commonSel } from "@ducks/common";
import { IExportTypes } from "@ducks/common/typings";
import { downloadProject } from "../../../utils/downloadProject";
import { useTranslation } from "react-i18next";
import ItemDepiction from "../../shared/ItemDepiction";
import { useProjectTaskTabsView } from "../projectTaskTabView/projectTaskTabsViewHooks";
import { wrapTooltip } from "../../../utils/uiUtils";
import { projectTagsRenderer } from "../ProjectTags/ProjectTags";
import { searchTagsRenderer } from "./SearchTags";
import { ArtefactTag } from "../ArtefactTag";

interface IProps {
    item: ISearchResultsServer;

    searchValue?: string;

    onOpenDeleteModal(item: ISearchResultsServer);

    onOpenDuplicateModal(item: ISearchResultsServer);

    onOpenCopyToModal(item: ISearchResultsServer);

    onRowClick?();

    toggleShowIdentifierModal(item: ISearchResultsServer);

    parentProjectId?: string;
}

export const searchItemLabel = (item: ISearchResultsServer) => item.label || item.id;

export default function SearchItem({
    item,
    searchValue,
    onOpenDeleteModal,
    onOpenDuplicateModal,
    onOpenCopyToModal,
    onRowClick,
    parentProjectId,
    toggleShowIdentifierModal,
}: IProps) {
    const dispatch = useDispatch();
    const exportTypes = useSelector(commonSel.exportTypesSelector);
    const [t] = useTranslation();
    const itemLinks = item.itemLinks ?? [{ path: "", label: "" }];
    // Remove detailsPath
    const menuItemLinks = itemLinks.slice(1);
    const { projectTabView, changeTab, menuItems } = useProjectTaskTabsView({
        srcLinks: menuItemLinks.map((link) => ({ ...link, id: link.label })),
        pluginId: item.pluginId,
        projectId: item.projectId,
        taskId: item.id,
    });
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
            </MenuItem>
        );
    }

    const goToDetailsPage = (e) => {
        // Only open page in same tab if user did not try to open in new tab
        if (!e?.ctrlKey && itemLinks.length > 0) {
            e.preventDefault();
            const detailsPath = itemLinks[0].path;
            const labels: IPageLabels = Object.create(null);
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
        downloadProject(item.id, type.id);
    };

    const projectOrDataset = item.type === "dataset" || item.type === "project";
    return (
        <Card isOnlyLayout className="diapp-searchitem">
            <OverviewItem hasSpacing onClick={onRowClick ? onRowClick : undefined} data-test-id={"search-item"}>
                <OverviewItemDepiction>
                    <ItemDepiction itemType={item.type} pluginId={item.pluginId} />
                </OverviewItemDepiction>
                <OverviewItemDescription>
                    <OverviewItemLine>
                        <h4>
                            <ResourceLink
                                url={!!itemLinks.length ? itemLinks[0].path : false}
                                handlerResourcePageLoader={!!itemLinks.length ? goToDetailsPage : false}
                            >
                                <OverflowText>
                                    <Highlighter label={searchItemLabel(item)} searchValue={searchValue} />
                                </OverflowText>
                            </ResourceLink>
                        </h4>
                        <Spacing vertical size="small" />
                        <OverflowText passDown={true} inline={true}>
                            {item.description &&
                                wrapTooltip(
                                    item.description.length > 80,
                                    <Markdown
                                        reHypePlugins={
                                            searchValue
                                                ? ([
                                                      markdownUtils.highlightSearchWordsPluginFactory(searchValue),
                                                  ] as MarkdownProps["reHypePlugins"])
                                                : undefined
                                        }
                                    >
                                        {item.description}
                                    </Markdown>,
                                    <Markdown
                                        inheritBlock
                                        allowedElements={["a", "mark"]}
                                        reHypePlugins={
                                            searchValue
                                                ? ([
                                                      markdownUtils.highlightSearchWordsPluginFactory(searchValue),
                                                  ] as MarkdownProps["reHypePlugins"])
                                                : undefined
                                        }
                                    >
                                        {item.description}
                                    </Markdown>
                                )}
                        </OverflowText>
                    </OverviewItemLine>
                    <OverviewItemLine small>
                        <TagList>
                            {projectOrDataset && (
                                <ArtefactTag artefactType={`${item.type}Node`}>
                                    <Highlighter
                                        label={t(
                                            "widget.Filterbar.subsections.valueLabels.itemType." + item.type,
                                            item.type[0].toUpperCase() + item.type.substr(1)
                                        )}
                                        searchValue={searchValue}
                                    />
                                </ArtefactTag>
                            )}
                            {item.type === DATA_TYPES.DATASET && item.readOnly && (
                                <Tag>
                                    <Icon name="state-locked" tooltipText={t("common.tooltips.dataset.readOnly")} />
                                </Tag>
                            )}
                            {item.pluginLabel && (
                                <ArtefactTag artefactType={`${item.pluginLabel.toLowerCase()}Node`}>
                                    <Highlighter label={item.pluginLabel} searchValue={searchValue} />
                                </ArtefactTag>
                            )}
                            {!parentProjectId && item.type !== DATA_TYPES.PROJECT && (
                                <Tag emphasis="weak">
                                    <Highlighter
                                        label={item.projectLabel ? item.projectLabel : item.projectId}
                                        searchValue={searchValue}
                                    />
                                </Tag>
                            )}
                            {projectTagsRenderer({ tags: item.tags, query: searchValue })}
                            {searchTagsRenderer({ searchTags: item.searchTags, searchText: searchValue })}
                        </TagList>
                    </OverviewItemLine>
                </OverviewItemDescription>
                <OverviewItemActions>
                    <IconButton
                        data-test-id={"open-duplicate-modal"}
                        name="item-clone"
                        text={t("common.action.clone", "Clone")}
                        onClick={() => onOpenDuplicateModal(item)}
                    />
                    {!!itemLinks.length && (
                        <IconButton
                            name="item-viewdetails"
                            text={t("common.action.showDetails", "Show details")}
                            onClick={goToDetailsPage}
                            href={itemLinks[0].path}
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
                        <MenuItem
                            icon="item-viewdetails"
                            text={t("common.action.showDetails", "Show details")}
                            key="view"
                            onClick={goToDetailsPage}
                            href={itemLinks[0].path}
                        />
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
                                            `projects/${item.id}/activities?page=1&limit=25&sortBy=recentlyUpdated&sortOrder=ASC`
                                        )
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
                </OverviewItemActions>
            </OverviewItem>
            {projectTabView}
        </Card>
    );
}
