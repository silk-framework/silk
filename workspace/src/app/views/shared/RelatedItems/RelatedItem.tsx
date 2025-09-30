import {
    ContextMenu,
    Highlighter,
    Icon,
    IconButton,
    MenuItem,
    OverviewItem,
    OverviewItemActions,
    OverviewItemDescription,
    OverviewItemLine,
    Spacing,
    TagList,
    Tooltip,
    Tag,
} from "@eccenca/gui-elements";
import { getItemLinkIcons } from "../../../utils/getItemLinkIcons";
import { ResourceLink } from "../ResourceLink/ResourceLink";
import React from "react";
import { IRelatedItem } from "@ducks/shared/typings";
import { useTranslation } from "react-i18next";
import { routerOp } from "@ducks/router";
import { useDispatch } from "react-redux";
import { useProjectTaskTabsView } from "../projectTaskTabView/projectTaskTabsViewHooks";
import { projectTagsRenderer } from "../ProjectTags/ProjectTags";
import { searchTagsRenderer } from "../SearchList/SearchTags";
import { ArtefactTag } from "../ArtefactTag";

interface IProps {
    // The related item to be shown
    relatedItem: IRelatedItem;
    // Optional text query to highlight matching text parts of the item.
    textQuery?: string;
}

export function RelatedItem({ relatedItem, textQuery }: IProps) {
    const [t] = useTranslation();
    const dispatch = useDispatch();
    const { projectTabView, changeTab, menuItems } = useProjectTaskTabsView({
        srcLinks: relatedItem.itemLinks.slice(1),
        pluginId: relatedItem.pluginId,
        projectId: relatedItem.projectId,
        taskId: relatedItem.id,
    });

    // Go to details page of related item
    const goToDetailsPage = (relatedItem: IRelatedItem, event) => {
        if (!event?.ctrlKey) {
            event.preventDefault();
            dispatch(
                // An item always has a details page link
                routerOp.goToPage(relatedItem.itemLinks[0].path, {
                    taskLabel: relatedItem.label,
                    itemType: relatedItem.type.toLowerCase(),
                }),
            );
        }
    };

    const otherMenuItems = relatedItem.itemLinks.map((link, idx) => (
        <MenuItem
            key={link.path}
            text={link.label}
            href={link.path}
            icon={getItemLinkIcons(link.label)}
            onClick={
                idx === 0
                    ? (e) => goToDetailsPage(relatedItem, e)
                    : (e) => {
                          e.preventDefault();
                          e.stopPropagation();
                          changeTab(link);
                      }
            }
        />
    ));

    const contextMenuItems = [otherMenuItems[0], ...menuItems, ...otherMenuItems.slice(1)];
    const itemTags = [] as React.JSX.Element[];
    if (relatedItem.type === "Dataset") {
        itemTags.push(
            <ArtefactTag key={"dataset"} artefactType="datasetNode">
                <Highlighter label={relatedItem.type} searchValue={textQuery} />
            </ArtefactTag>,
        );
    }
    if (relatedItem.readOnly) {
        itemTags.push(
            <Tag key={"readOnlyTag"}>
                <Icon name="state-locked" tooltipText={t("common.tooltips.dataset.readOnly")} />
            </Tag>,
        );
    }
    itemTags.push(
        <ArtefactTag key={relatedItem.pluginLabel} artefactType={`${relatedItem.pluginLabel.toLowerCase()}Node`}>
            <Highlighter label={relatedItem.pluginLabel} searchValue={textQuery} />
        </ArtefactTag>,
    );
    return (
        <OverviewItem key={relatedItem.id}>
            <OverviewItemDescription>
                <OverviewItemLine small>
                    <ResourceLink
                        url={!!relatedItem.itemLinks.length ? relatedItem.itemLinks[0].path : false}
                        handlerResourcePageLoader={
                            !!relatedItem.itemLinks.length ? (e) => goToDetailsPage(relatedItem, e) : false
                        }
                    >
                        <Highlighter label={relatedItem.label} searchValue={textQuery} />
                    </ResourceLink>
                </OverviewItemLine>
                <OverviewItemLine small>
                    <TagList>
                        {itemTags}
                        {projectTagsRenderer({
                            tags: relatedItem.tags,
                            query: textQuery,
                        })}
                        {searchTagsRenderer({
                            searchTags: relatedItem.searchTags,
                            searchText: textQuery,
                        })}
                    </TagList>
                </OverviewItemLine>
            </OverviewItemDescription>
            <OverviewItemActions>
                {!!relatedItem.itemLinks.length && (
                    <IconButton
                        name="item-viewdetails"
                        text={t("common.action.showDetails", "Show details")}
                        onClick={(e) => goToDetailsPage(relatedItem, e)}
                        href={relatedItem.itemLinks[0].path}
                    />
                )}
                {contextMenuItems.length > 1 && ( // Only show context menu when more than the detail page is included
                    <ContextMenu
                        data-test-id={"related-item-context-menu"}
                        togglerText={t("common.action.moreOptions", "Show more options")}
                    >
                        {contextMenuItems}
                    </ContextMenu>
                )}
            </OverviewItemActions>
            {projectTabView ?? null}
        </OverviewItem>
    );
}
