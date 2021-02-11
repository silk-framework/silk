import React from "react";
import {
    Highlighter,
    OverflowText,
    Spacing,
    Toolbar,
    ToolbarSection,
} from "@gui-elements/index";
import { SourcePathInfoBox } from "./SourcePathInfoBox";

interface IProps {
    label: string;
    search?: string;
}

export function SourceCellData({label, search}: IProps) {
    return <Toolbar noWrap={true}>
        <ToolbarSection canShrink={true}>
            <OverflowText ellipsis={"reverse"}><Highlighter label={label} searchValue={search}/></OverflowText>
        </ToolbarSection>
        <ToolbarSection>
            <Spacing vertical={true} size="tiny" />
            <SourcePathInfoBox source={label}/>
        </ToolbarSection>
    </Toolbar>
}
