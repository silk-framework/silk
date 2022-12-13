import { Highlighter, OverflowText, Spacing, Toolbar, ToolbarSection, Tooltip } from "@eccenca/gui-elements";
import React from "react";

import { SuggestionTypeValues } from "../../suggestion.typings";
import { SourcePathInfoBox } from "./SourcePathInfoBox";

interface IProps {
    label: string;
    search?: string;
    pathType?: SuggestionTypeValues;
    objectInfo?: {
        dataTypeSubPaths: string[];
        objectSubPaths: string[];
    };
}

export function SourceCellData({ label, search, pathType, objectInfo }: IProps) {
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
            <ToolbarSection canShrink={true}>{labelElem}</ToolbarSection>
            <ToolbarSection>
                <Spacing vertical={true} size="tiny" />
                <SourcePathInfoBox source={label} pathType={pathType} objectInfo={objectInfo} />
            </ToolbarSection>
        </Toolbar>
    );
}
