import React from "react";
import { ISearchResultsServer } from "@ducks/workspace/typings";
import {
    cn,
    Highlighter,
    OverflowText,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
    TagList,
} from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import ItemDepiction from "../../shared/ItemDepiction";
import { ResourceLink } from "../ResourceLink/ResourceLink";
import { DATA_TYPES } from "../../../constants";
import { searchItemLabel } from "./SearchItem";
import SearchItemActions, { SearchItemActionsProps } from "./SearchItemActions";
import { searchItemTagsRenderer } from "./SearchItemTags";
import { stripMarkdown } from "./stripMarkdown";
import { useItemNavigation } from "./useItemNavigation";

type ItemCallbacks = Omit<SearchItemActionsProps, "item">;

interface SearchTableProps extends ItemCallbacks {
    data: ISearchResultsServer[];
    searchValue?: string;
    /** When set, the item belongs to this project and the "Project" column is hidden. */
    parentProjectId?: string;
    /** Render without the surrounding bordered panel — used when the table is embedded inside a card. */
    flush?: boolean;
}

interface SearchTableRowProps extends ItemCallbacks {
    item: ISearchResultsServer;
    searchValue?: string;
    parentProjectId?: string;
    showProjectColumn: boolean;
}

const itemTypeLabel = (item: ISearchResultsServer, t: (key: string, fallback?: string) => string): string =>
    // Results from other list pages sharing the search state (e.g. activities) have no `type`.
    item.type
        ? t(
              "widget.Filterbar.subsections.valueLabels.itemType." + item.type,
              item.type[0].toUpperCase() + item.type.substr(1),
          )
        : "";

function SearchTableRow({ item, searchValue, parentProjectId, showProjectColumn, ...callbacks }: SearchTableRowProps) {
    const [t] = useTranslation();
    const { itemLinks, goToDetailsPage } = useItemNavigation(item);
    const tags = searchItemTagsRenderer({
        item,
        searchValue,
        parentProjectId,
        includeType: false,
        includePlugin: false,
        includeProject: false,
        t,
    });

    return (
        <TableRow data-test-id="search-item">
            <TableCell alignVertical="middle">
                <div className="flex items-center gap-2">
                    <span className="flex size-5 shrink-0 items-center justify-center">
                        <ItemDepiction itemType={item.type} pluginId={item.pluginId} size={{ small: true }} />
                    </span>
                    <span className="whitespace-nowrap text-muted-foreground">
                        <Highlighter label={itemTypeLabel(item, t)} searchValue={searchValue} />
                    </span>
                </div>
            </TableCell>
            <TableCell alignVertical="middle">
                <div className="max-w-[20rem] font-medium">
                    <ResourceLink
                        url={itemLinks.length ? itemLinks[0].path : false}
                        handlerResourcePageLoader={itemLinks.length ? goToDetailsPage : false}
                    >
                        <OverflowText>
                            <Highlighter label={searchItemLabel(item)} searchValue={searchValue} />
                        </OverflowText>
                    </ResourceLink>
                </div>
            </TableCell>
            <TableCell alignVertical="middle">
                {item.description ? (
                    <div className="max-w-[24rem] text-muted-foreground">
                        <OverflowText>
                            {/* Descriptions may contain markdown; the truncated cell shows them as plain text. */}
                            <Highlighter label={stripMarkdown(item.description)} searchValue={searchValue} />
                        </OverflowText>
                    </div>
                ) : null}
            </TableCell>
            <TableCell alignVertical="middle">
                {item.pluginLabel ? (
                    <span className="whitespace-nowrap">
                        <Highlighter label={item.pluginLabel} searchValue={searchValue} />
                    </span>
                ) : null}
            </TableCell>
            {showProjectColumn ? (
                <TableCell alignVertical="middle">
                    {item.type !== DATA_TYPES.PROJECT ? (
                        <div className="max-w-[14rem]">
                            <OverflowText>
                                <Highlighter
                                    label={item.projectLabel ? item.projectLabel : (item.projectId ?? "")}
                                    searchValue={searchValue}
                                />
                            </OverflowText>
                        </div>
                    ) : null}
                </TableCell>
            ) : null}
            <TableCell alignVertical="middle">{tags.length ? <TagList>{tags}</TagList> : null}</TableCell>
            <TableCell alignVertical="middle">
                <div className="flex items-center justify-end gap-1">
                    <SearchItemActions item={item} {...callbacks} />
                </div>
            </TableCell>
        </TableRow>
    );
}

/** Tabular presentation of the search results, with a dedicated column per property. */
export default function SearchTable({
    data,
    searchValue,
    parentProjectId,
    flush = false,
    ...callbacks
}: SearchTableProps) {
    const [t] = useTranslation();
    const showProjectColumn = !parentProjectId;

    return (
        <div
            className={cn(
                "overflow-x-auto",
                // Standalone (e.g. /workbench) the table is its own bordered panel; embedded in a card
                // (the Project "Contents" tile) it renders flush to avoid a card-inside-a-card.
                !flush && "rounded-lg border border-border bg-card",
            )}
            data-test-id="search-result-table"
        >
            <Table size="medium" hasDivider>
                <TableHead>
                    <TableRow>
                        <TableHeader>{t("widget.SearchTable.columns.type", "Type")}</TableHeader>
                        <TableHeader>{t("widget.SearchTable.columns.name", "Name")}</TableHeader>
                        <TableHeader>{t("widget.SearchTable.columns.description", "Description")}</TableHeader>
                        <TableHeader>{t("widget.SearchTable.columns.plugin", "Plugin")}</TableHeader>
                        {showProjectColumn ? (
                            <TableHeader>{t("widget.SearchTable.columns.project", "Project")}</TableHeader>
                        ) : null}
                        <TableHeader>{t("widget.SearchTable.columns.tags", "Tags")}</TableHeader>
                        <TableHeader>
                            <span className="sr-only">{t("widget.SearchTable.columns.actions", "Actions")}</span>
                        </TableHeader>
                    </TableRow>
                </TableHead>
                <TableBody>
                    {data.map((item) => (
                        <SearchTableRow
                            key={`${item.id}_${item.projectId}`}
                            item={item}
                            searchValue={searchValue}
                            parentProjectId={parentProjectId}
                            showProjectColumn={showProjectColumn}
                            {...callbacks}
                        />
                    ))}
                </TableBody>
            </Table>
        </div>
    );
}
