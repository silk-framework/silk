import React from "react";
import { ISearchResultsServer } from "@ducks/workspace/typings";
import { Card, Highlighter, Markdown, MarkdownProps, markdownUtils } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import ItemDepiction from "../../shared/ItemDepiction";
import { ResourceLink } from "../ResourceLink/ResourceLink";
import { searchItemLabel } from "./SearchItem";
import SearchItemActions, { SearchItemActionsProps } from "./SearchItemActions";
import SearchItemTags from "./SearchItemTags";
import { useItemNavigation } from "./useItemNavigation";

export interface SearchCardProps extends Omit<SearchItemActionsProps, "item" | "compact"> {
    item: ISearchResultsServer;
    searchValue?: string;
    parentProjectId?: string;
}

/** A single search result rendered as a vertical card, used by the grid presentation. */
export default function SearchCard({ item, searchValue, parentProjectId, ...callbacks }: SearchCardProps) {
    const [t] = useTranslation();
    const { itemLinks, goToDetailsPage } = useItemNavigation(item);

    return (
        <Card isOnlyLayout className="diapp-searchcard flex h-full flex-col gap-3 p-4">
            <div className="flex items-start gap-3">
                <span className="flex size-8 shrink-0 items-center justify-center rounded-md bg-muted">
                    <ItemDepiction itemType={item.type} pluginId={item.pluginId} size={{ small: true }} />
                </span>
                <h4 className="line-clamp-2 min-w-0 flex-1 text-sm font-medium leading-snug break-words">
                    <ResourceLink
                        url={itemLinks.length ? itemLinks[0].path : false}
                        handlerResourcePageLoader={itemLinks.length ? goToDetailsPage : false}
                    >
                        <Highlighter label={searchItemLabel(item)} searchValue={searchValue} />
                    </ResourceLink>
                </h4>
                <div className="-mt-1 -mr-1 flex shrink-0 items-center">
                    <SearchItemActions item={item} compact {...callbacks} />
                </div>
            </div>
            <div className="text-sm text-muted-foreground">
                {item.description ? (
                    <div className="line-clamp-3">
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
                    </div>
                ) : (
                    <span className="italic">{t("common.messages.noDescription", "No description")}</span>
                )}
            </div>
            <div className="mt-auto">
                <SearchItemTags item={item} searchValue={searchValue} parentProjectId={parentProjectId} />
            </div>
        </Card>
    );
}
