import React from "react";
import { ISearchResultsServer } from "@ducks/workspace/typings";
import {
    Card,
    Highlighter,
    Markdown,
    MarkdownProps,
    OverflowText,
    OverviewItem,
    OverviewItemActions,
    OverviewItemDepiction,
    OverviewItemDescription,
    OverviewItemLine,
    Spacing,
    markdownUtils,
} from "@eccenca/gui-elements";
import { ResourceLink } from "../ResourceLink/ResourceLink";
import ItemDepiction from "../../shared/ItemDepiction";
import { wrapTooltip } from "../../../utils/uiUtils";
import SearchItemActions from "./SearchItemActions";
import SearchItemTags from "./SearchItemTags";
import { useItemNavigation } from "./useItemNavigation";

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
    const { itemLinks, goToDetailsPage } = useItemNavigation(item);

    return (
        <Card isOnlyLayout className="diapp-searchitem">
            <OverviewItem hasSpacing onClick={onRowClick ? onRowClick : undefined} data-test-id={"search-item"}>
                <OverviewItemDepiction>
                    {/* 16px item icon, matching the table/grid views (was the 32px `large` default). */}
                    <ItemDepiction itemType={item.type} pluginId={item.pluginId} size={{ small: true }} />
                </OverviewItemDepiction>
                <OverviewItemDescription>
                    <OverviewItemLine>
                        <h4 className="font-medium">
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
                                    </Markdown>,
                                )}
                        </OverflowText>
                    </OverviewItemLine>
                    <OverviewItemLine small>
                        <SearchItemTags item={item} searchValue={searchValue} parentProjectId={parentProjectId} />
                    </OverviewItemLine>
                </OverviewItemDescription>
                <OverviewItemActions>
                    <SearchItemActions
                        item={item}
                        onOpenDeleteModal={onOpenDeleteModal}
                        onOpenDuplicateModal={onOpenDuplicateModal}
                        onOpenCopyToModal={onOpenCopyToModal}
                        toggleShowIdentifierModal={toggleShowIdentifierModal}
                    />
                </OverviewItemActions>
            </OverviewItem>
        </Card>
    );
}
