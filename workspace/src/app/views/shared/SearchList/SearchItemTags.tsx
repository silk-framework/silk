import React from "react";
import { ISearchResultsServer } from "@ducks/workspace/typings";
import { Highlighter, Icon, Tag, TagList } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { DATA_TYPES } from "../../../constants";
import { projectTagsRenderer } from "../ProjectTags/ProjectTags";
import { searchTagsRenderer } from "./SearchTags";
import { ArtefactTag } from "../ArtefactTag";

interface IProps {
    item: ISearchResultsServer;
    searchValue?: string;
    parentProjectId?: string;
    /** Render the item type badge. */
    includeType?: boolean;
    /** Render the read-only lock badge. Kept separate from `includeType` because the table view
     *  has a dedicated type column but no other place for the read-only marker. */
    includeReadOnly?: boolean;
    /** Render the plugin badge. */
    includePlugin?: boolean;
    /** Render the containing project badge. */
    includeProject?: boolean;
    /** Wrap the tags in a `TagList`. Set to false to embed the tags in a custom container. */
    withList?: boolean;
}

/**
 * The badge/tag cluster of a search result item (type, read-only marker, plugin, containing
 * project, user tags and search tags). Shared by the row card, table row and grid card; the
 * `include*` flags let a presentation drop badges it already renders as dedicated columns.
 */
export const searchItemTagsRenderer = ({
    item,
    searchValue,
    parentProjectId,
    includeType = true,
    includeReadOnly = true,
    includePlugin = true,
    includeProject = true,
    t,
}: IProps & { t: (key: string, fallbackOrOpts?: any) => string }): React.JSX.Element[] => {
    const projectOrDataset = item.type === "dataset" || item.type === "project";
    const tags: React.JSX.Element[] = [];

    if (includeType && projectOrDataset) {
        tags.push(
            <ArtefactTag key="type" artefactType={`${item.type}-node`}>
                <Highlighter
                    label={t(
                        "widget.Filterbar.subsections.valueLabels.itemType." + item.type,
                        item.type[0].toUpperCase() + item.type.substr(1),
                    )}
                    searchValue={searchValue}
                />
            </ArtefactTag>,
        );
    }
    if (includeReadOnly && item.type === DATA_TYPES.DATASET && item.readOnly) {
        tags.push(
            <Tag key="readonly">
                <Icon name="state-locked" small tooltipText={t("common.tooltips.dataset.readOnly")} />
            </Tag>,
        );
    }
    if (includePlugin && item.pluginLabel) {
        tags.push(
            <ArtefactTag key="plugin" artefactType={`${item.pluginLabel.toLowerCase()}-node`}>
                <Highlighter label={item.pluginLabel} searchValue={searchValue} />
            </ArtefactTag>,
        );
    }
    if (includeProject && !parentProjectId && item.type !== DATA_TYPES.PROJECT) {
        tags.push(
            <Tag key="project" emphasis="weak">
                <Highlighter label={item.projectLabel ? item.projectLabel : item.projectId} searchValue={searchValue} />
            </Tag>,
        );
    }
    tags.push(...projectTagsRenderer({ tags: item.tags, query: searchValue }));
    tags.push(...searchTagsRenderer({ searchTags: item.searchTags, searchText: searchValue }));
    return tags;
};

/** Component wrapper around {@link searchItemTagsRenderer}. */
export default function SearchItemTags({ withList = true, ...props }: IProps) {
    const [t] = useTranslation();
    const tags = searchItemTagsRenderer({ ...props, t });
    if (!tags.length) {
        return null;
    }
    return withList ? <TagList>{tags}</TagList> : <>{tags}</>;
}
