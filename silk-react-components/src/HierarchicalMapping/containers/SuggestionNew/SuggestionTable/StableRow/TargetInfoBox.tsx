import { ContextMenu } from "@gui-elements/index";
import React, { useContext } from "react";
import { IPageSuggestion, ITargetWithSelected } from "../../suggestion.typings";
import { SuggestionListContext } from "../../SuggestionContainer";

interface IProps {
    selectedTarget: ITargetWithSelected | IPageSuggestion;
}

export default function TargetInfoBox({selectedTarget}: IProps) {
    const context = useContext(SuggestionListContext);
    const {portalContainer} = context;

    return <ContextMenu
        portalContainer={portalContainer}
        togglerElement={'item-info'}
    >
        <ul>
            <li><b>Label:</b> {selectedTarget.label}</li>
            <li><b>Description:</b> {selectedTarget.description}</li>
            <li><b>Uri:</b> {selectedTarget.uri}</li>
            <li><b>Link:</b>
                <a href={selectedTarget.link || 'http://dummy.link'}
                   target='_blank'>{selectedTarget.link || 'http://dummy.link'}
                </a>
            </li>
        </ul>
    </ContextMenu>

}
