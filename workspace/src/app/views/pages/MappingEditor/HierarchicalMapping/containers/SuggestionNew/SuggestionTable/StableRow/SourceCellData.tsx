import React from "react";
import {
    Highlighter,
    OverflowText,
    OverviewItem,
    OverviewItemDescription,
    OverviewItemLine,
    Spacing,
    Toolbar,
    ToolbarSection,
    Tooltip,
} from "@eccenca/gui-elements";
import { SourcePathInfoBox } from "./SourcePathInfoBox";
import { SuggestionTypeValues } from "../../suggestion.typings";

interface IProps {
    label: string;
    /** The path from the object source to the source element. */
    sourcePath?: string;
    search?: string;
    pathType?: SuggestionTypeValues;
    objectInfo?: {
        dataTypeSubPaths: string[];
        objectSubPaths: string[];
    };
}

export function SourceCellData({ label, sourcePath, search, pathType, objectInfo }: IProps) {
    let labelElem = (
        <OverflowText ellipsis={"reverse"} inline={true}>
            <Highlighter label={label} searchValue={search} />
        </OverflowText>
    );
    if (label.length > 20) {
        labelElem = (
            <Tooltip size="large" content={label}>
                {labelElem}
            </Tooltip>
        );
    }
    return (
        <Toolbar noWrap={true}>
            <ToolbarSection canShrink={true}>
                {sourcePath && sourcePath !== label ? (
                    <OverviewItem>
                        <OverviewItemDescription>
                            <OverviewItemLine>{label}</OverviewItemLine>
                            <OverviewItemLine>{sourcePath}</OverviewItemLine>
                        </OverviewItemDescription>
                    </OverviewItem>
                ) : (
                    labelElem
                )}
            </ToolbarSection>
            <ToolbarSection>
                <Spacing vertical={true} size="tiny" />
                <SourcePathInfoBox source={label} pathType={pathType} objectInfo={objectInfo} />
            </ToolbarSection>
        </Toolbar>
    );
}
