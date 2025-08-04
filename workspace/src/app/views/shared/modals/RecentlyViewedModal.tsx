import React, { useEffect, useState } from "react";
import {
    SuggestField,
    suggestFieldUtils,
    Button,
    Highlighter,
    highlighterUtils,
    Notification,
    OverflowText,
    OverviewItem,
    OverviewItemDepiction,
    OverviewItemDescription,
    OverviewItemLine,
    SimpleDialog,
    Spacing,
    Icon,
    SuggestFieldItemRendererModifierProps,
    Tag,
    TagList,
} from "@eccenca/gui-elements";
import { CLASSPREFIX as eccguiprefix } from "@eccenca/gui-elements/src/configuration/constants";
import useHotKey from "../HotKeyHandler/HotKeyHandler";
import { useTranslation } from "react-i18next";
import { recentlyViewedItems } from "@ducks/workspace/requests";
import { IRecentlyViewedItem } from "@ducks/workspace/typings";
import { ErrorResponse } from "../../../services/fetch/responseInterceptor";
import { Loading } from "../Loading/Loading";
import { IItemLink } from "@ducks/shared/typings";
import { useDispatch, useSelector } from "react-redux";
import { routerOp } from "@ducks/router";
import { useLocation } from "react-router";
import { commonSel } from "@ducks/common";
import { absolutePageUrl } from "@ducks/router/operations";
import { ItemDepiction } from "../ItemDepiction/ItemDepiction";
import { uppercaseFirstChar } from "../../../utils/transformers";
import { projectTagsRenderer } from "../ProjectTags/ProjectTags";
import { searchTagsRenderer } from "../SearchList/SearchTags";
import { ArtefactTag } from "../ArtefactTag";

/** Shows the recently viewed items a user has visited. Also allows to trigger a workspace search. */
export function RecentlyViewedModal() {
    // Flag if this modal is shown
    const [isOpen, setIsOpen] = useState(false);
    // The recent item list as fetched from the backend. The search is done over this list.
    const [recentItems, setRecentItems] = useState<IRecentlyViewedItem[]>([]);
    // Loading spinner when a request is pending
    const [loading, setLoading] = useState(true);
    // Error response from the REST backend
    const [error, setError] = useState<ErrorResponse | null>(null);
    // Current path name
    const { pathname } = useLocation();
    const { t } = useTranslation();
    const dispatch = useDispatch();
    const { hotKeys } = useSelector(commonSel.initialSettingsSelector);
    const loadRecentItems = async () => {
        setError(null);
        try {
            setLoading(true);
            const recentItems = (await recentlyViewedItems()).data;
            if (
                recentItems.length > 1 &&
                recentItems[0].itemLinks.length > 0 &&
                recentItems[0].itemLinks[0].path.endsWith(pathname)
            ) {
                // swap 1. and 2. result if 1. result is the same page we are already on
                [recentItems[0], recentItems[1]] = [recentItems[1], recentItems[0]];
            }
            setRecentItems(recentItems);
        } catch (ex) {
            if (ex.isFetchError && ex.errorResponse) {
                setError(ex.errorResponse);
            } else {
                throw ex;
            }
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (isOpen && hotKeys.quickSearch) {
            loadRecentItems();
        }
    }, [isOpen, hotKeys.quickSearch]);

    useHotKey({
        hotkey: hotKeys.quickSearch,
        handler: () => {
            setIsOpen(true);
            return false; // prevent default
        },
    });
    const triggerShiftUp = () => {
        // This is needed, because e.g. on German keyboards the SHIFT keyup of the hotkey will be send from the input element of the quick search, which is ignored by e.g. react-flow
        const customShiftUpEvent = new KeyboardEvent("keyup", { key: "Shift", bubbles: true });
        const body = document.querySelector("body");
        body && body.dispatchEvent(customShiftUpEvent);
    };
    const close = () => {
        setRecentItems([]);
        setIsOpen(false);
        triggerShiftUp();
    };
    const onChange = (itemLinks: IItemLink[], e) => {
        if (e) {
            e.preventDefault();
        }
        setIsOpen(false);
        if (itemLinks.length > 0) {
            const itemLink = itemLinks[0];
            dispatch(routerOp.goToPage(itemLink.path));
        }
    };
    // The string representation of a recently viewed item that can be full text searched
    const itemSearchableString = (item: IRecentlyViewedItem) => {
        const projectLabel = item.projectLabel ? item.projectLabel : item.projectId;
        const taskLabel = item.taskLabel ? item.taskLabel : item.taskId;
        const label = taskLabel ? `${taskLabel} ${projectLabel} ${item.pluginLabel}` : projectLabel;
        return `${label} ${itemType(item)} ${item.searchTags?.join(" ")} ${item.tags?.map((t) => t.label).join(" ")}`;
    };
    const itemType = (item: IRecentlyViewedItem): string => uppercaseFirstChar(t("common.dataTypes." + item.itemType));
    // The representation of an item as an option in the selection list
    const itemOption = (
        item: IRecentlyViewedItem,
        query: string,
        modifiers: SuggestFieldItemRendererModifierProps,
        handleSelectClick: () => any
    ) => {
        console.log("quick search item", item);
        const label = item.taskLabel || item.taskId || item.projectLabel || item.projectId;
        return (
            <OverviewItem
                className={
                    "diapp-recentlyviewedmodal__searchitem" +
                    (modifiers.active ? ` ${eccguiprefix}-overviewitem__item--active` : "")
                }
                key={item.projectId + item.taskId}
                hasSpacing
                onClick={handleSelectClick}
            >
                <OverviewItemDepiction>
                    <ItemDepiction itemType={item.itemType} pluginId={item.pluginId} />
                </OverviewItemDepiction>
                <OverviewItemDescription style={{ maxWidth: "50vw" }}>
                    <OverviewItemLine>
                        <h4>
                            <OverflowText inline={true} style={{ width: "100vw" }}>
                                <Highlighter label={label} searchValue={query} />
                            </OverflowText>
                        </h4>
                    </OverviewItemLine>
                    <OverviewItemLine small>
                        <TagList>
                            {(item.itemType === "project" || item.itemType === "dataset") && (
                                <ArtefactTag artefactType={`${item.itemType}Node`}>
                                    <Highlighter label={itemType(item)} searchValue={query} />
                                </ArtefactTag>
                            )}
                            {item.itemType === "dataset" && item.readOnly && (
                                <Tag>
                                    <Icon name="state-locked" tooltipText={t("common.tooltips.dataset.readOnly")} />
                                </Tag>
                            )}
                            {item.pluginLabel && (
                                <ArtefactTag artefactType={`${item.pluginLabel?.toLowerCase()}Node`}>
                                    <Highlighter label={item.pluginLabel} searchValue={query} />
                                </ArtefactTag>
                            )}
                            {item.taskId && (
                                <Tag emphasis={"weak"}>
                                    <Highlighter
                                        label={item.projectLabel ? item.projectLabel : item.projectId}
                                        searchValue={query}
                                    />
                                </Tag>
                            )}
                            {projectTagsRenderer({ tags: item.tags, query })}
                            {searchTagsRenderer({ searchTags: item.searchTags, searchText: query })}
                        </TagList>
                    </OverviewItemLine>
                </OverviewItemDescription>
            </OverviewItem>
        );
    };
    // Searches on the results from the initial requests
    const handleSearch = (textQuery: string) => {
        const searchWords = highlighterUtils.extractSearchWords(textQuery.toLowerCase());
        return recentItems.filter((item) => {
            const label = itemSearchableString(item).toLowerCase();
            return searchWords.every((word) => label.includes(word));
        });
    };
    // Warning when an error has occurred
    const errorView = (error: ErrorResponse) => {
        return (
            <Notification danger>
                <span>
                    {error.title}. {error.detail ? ` Details: ${error.detail}` : ""}
                </span>
            </Notification>
        );
    };
    // Global search action
    const globalSearch: (string) => IRecentlyViewedItem = (query: string) => {
        return {
            projectId: "",
            projectLabel: "",
            itemType: "",
            itemLinks: [
                {
                    id: "workspaceSearch",
                    label: "Search workspace",
                    path: absolutePageUrl("?textQuery=" + encodeURIComponent(query)),
                },
            ],
        };
    };
    // Displays the 'search in workspace' option in the list.
    const createNewItemRenderer = suggestFieldUtils.createNewItemRendererFactory(
        (query) => t("RecentlyViewedModal.globalSearch", { query }),
        "operation-search"
    );

    // The auto-completion of the recently viewed items
    const recentlyViewedAutoCompletion = () => {
        return (
            <SuggestField<IRecentlyViewedItem, IItemLink[]>
                onSearch={handleSearch}
                itemValueSelector={(value) => value.itemLinks}
                itemRenderer={itemOption}
                onChange={onChange}
                autoFocus={true}
                inputProps={{
                    placeholder: t("RecentlyViewedModal.placeholder"),
                }}
                itemValueString={(item) => `${item.projectId}_${item.taskId}`}
                createNewItem={{
                    itemFromQuery: globalSearch,
                    itemRenderer: createNewItemRenderer,
                }}
                contextOverlayProps={{ placement: "bottom-start", rootBoundary: "viewport" }}
                // This is used for the key generation of the option React elements, even though this is not displayed anywhere.
                itemValueRenderer={(item) => `${item.projectId} ${item.taskId ? item.taskId : ""}`}
                noResultText={t("common.messages.noResults")}
            />
        );
    };
    return (
        <SimpleDialog
            className="diapp-recentlyviewedmodal"
            data-test-id={"quick-search-modal"}
            transitionDuration={20}
            onClose={close}
            isOpen={isOpen}
            title={t("RecentlyViewedModal.title")}
            actions={
                <Button data-test-id={"close-quick-search-modal-btn"} onClick={close}>
                    {t("common.action.close")}
                </Button>
            }
            forceTopPosition
        >
            {loading ? <Loading delay={0} /> : error ? errorView(error) : recentlyViewedAutoCompletion()}
        </SimpleDialog>
    );
}
