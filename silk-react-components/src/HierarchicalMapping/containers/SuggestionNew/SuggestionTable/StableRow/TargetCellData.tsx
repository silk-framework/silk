import React from "react";
import { Highlighter, OverflowText } from "@gui-elements/index";
import { IPageSuggestion } from "../../suggestion.typings";
import TargetInfoBox from "./TargetInfoBox";

interface IProps {
    search?: string;

    target: IPageSuggestion;
}

export function TargetCellData({target, search}: IProps) {
    return <>
        <p><OverflowText><Highlighter label={target.label} searchValue={search}/></OverflowText></p>
        <p><OverflowText><Highlighter label={target.source} searchValue={search}/></OverflowText></p>
        {target.description &&
        <p><OverflowText><Highlighter label={target.description} searchValue={search}/></OverflowText></p>}
        <TargetInfoBox selectedTarget={target} />
    </>
}
