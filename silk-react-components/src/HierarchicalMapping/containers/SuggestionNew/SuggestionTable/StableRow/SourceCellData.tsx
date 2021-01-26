import React from "react";
import {
    Highlighter,
    OverflowText,
    Spacing,
    Toolbar,
    ToolbarSection,
} from "@gui-elements/index";
import { ExampleInfoBox } from "./ExampleInfoBox";

interface IProps {
    label: string;
    search?: string;
}

export function SourceCellData({label, search}: IProps) {
    return <Toolbar noWrap={true}>
        <ToolbarSection canShrink={true}>
            <OverflowText><Highlighter label={label} searchValue={search}/></OverflowText>
        </ToolbarSection>
        <ToolbarSection>
            <Spacing vertical={true} size="tiny" />
            <ExampleInfoBox source={label}/>
        </ToolbarSection>
    </Toolbar>
}
