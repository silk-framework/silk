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
    const dmBaseUrl = context.frontendInitData?.dmBaseUrl
    const dmResourceLink = () => {
        return `${context.frontendInitData.dmBaseUrl}/explore?resource=${encodeURIComponent(selectedTarget.uri)}&graph=${encodeURIComponent(selectedTarget.graph)}`
    }

    return <ContextMenu
        portalContainer={portalContainer}
        togglerElement={'item-info'}
    >
        <ul>
            <li><b>Label:</b> {selectedTarget.label}</li>
            <li><b>Description:</b> {selectedTarget.description}</li>
            {selectedTarget.uri && <li><b>Uri:</b> {selectedTarget.uri}</li>}
            {dmBaseUrl && selectedTarget.graph && <li><b>Show in DataManager:&nbsp;</b>
                <a href={dmResourceLink()}
                   target='_blank'>{selectedTarget.label}
                </a>
            </li>}
        </ul>
    </ContextMenu>

}
