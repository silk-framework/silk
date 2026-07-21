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
import { AppDispatch } from "store/configureStore";

interface IProps {
    // The related item to be shown
    relatedItem: IRelatedItem;
    // Optional text query to highlight matching text parts of the item.
    textQuery?: string;
}

export function RelatedItem({ relatedItem, textQuery }: IProps) {
    const [t] = useTranslation();
    const dispatch = useDispatch<AppDispatch>();
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
            <ArtefactTag key={"dataset"} artefactType="dataset-node">
                <Highlighter label={relatedItem.type} searchValue={textQuery} />
            </ArtefactTag>,
        );
    }
    if (relatedItem.readOnly) {
        itemTags.push(
            <Tag key={"readOnlyTag"}>
                <Icon name="state-locked" small tooltipText={t("common.tooltips.dataset.readOnly")} />
            </Tag>,
        );
    }
    itemTags.push(
        <ArtefactTag key={relatedItem.pluginLabel} artefactType={`${relatedItem.pluginLabel.toLowerCase()}-node`}>
            <Highlighter label={relatedItem.pluginLabel} searchValue={textQuery} />
        </ArtefactTag>,
    );
    const allTags = [
        ...itemTags,
        ...projectTagsRenderer({ tags: relatedItem.tags, query: textQuery }),
        ...searchTagsRenderer({ searchTags: relatedItem.searchTags, searchText: textQuery }),
    ];
    return (
        <OverviewItem key={relatedItem.id} className="items-center">
            <OverviewItemDescription>
                {/* Title line */}
                <OverviewItemLine className="font-medium">
                    <ResourceLink
                        url={!!relatedItem.itemLinks.length ? relatedItem.itemLinks[0].path : false}
                        handlerResourcePageLoader={
                            !!relatedItem.itemLinks.length ? (e) => goToDetailsPage(relatedItem, e) : false
                        }
                    >
                        <Highlighter label={relatedItem.label} searchValue={textQuery} />
                    </ResourceLink>
                </OverviewItemLine>
                {/* Badge row: explicit flex/gap so spacing is guaranteed regardless of TagList internals.
                    `mt-2` separates the badges from the title, `gap-2` spaces the badges from each other. */}
                {allTags.length > 0 && <div className="mt-2 flex flex-wrap items-center gap-2">{allTags}</div>}
            </OverviewItemDescription>
            <OverviewItemActions>
                {!!relatedItem.itemLinks.length && (
                    // `item-launch` (open/launch glyph) fits "open the details page" better than the
                    // ambiguous eye; `text` is surfaced as the hover tooltip. `href` keeps it a real
                    // anchor so ctrl/cmd-click opens the page in a new tab.
                    <IconButton
                        name="item-launch"
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
