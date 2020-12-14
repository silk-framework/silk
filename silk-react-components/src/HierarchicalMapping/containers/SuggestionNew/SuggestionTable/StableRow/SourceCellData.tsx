import React from "react";
import { Highlighter, OverflowText } from "@gui-elements/index";
import { ExampleInfoBox } from "./ExampleInfoBox";

interface IProps {
    label: string;

    search?: string;
}

export function SourceCellData({label, search}: IProps) {
    return <>
        <OverflowText><Highlighter label={label} searchValue={search}/></OverflowText>
        <ExampleInfoBox source={label}/>
    </>
}
