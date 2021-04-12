import React, { useContext } from "react";
import { IPageSuggestion, ITargetWithSelected } from "../../suggestion.typings";
import { SuggestionListContext } from "../../SuggestionContainer";
import { InfoBoxOverlay } from "./InfoBoxOverlay";

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

    return <InfoBoxOverlay
        data={[
            {
                key: "Label",
                value: selectedTarget.label,
            },
            {
                key: "URI",
                value: (<span style={{ wordBreak: "break-all" }}>{ selectedTarget.uri }</span>),
            },
            {
                key: "Explore in DataManager",
                value: (dmBaseUrl && selectedTarget.graph) ? <a href={dmResourceLink()}
                   target='_blank'>{selectedTarget.label}
                </a> : '',
            },
            {
                key: "Description",
                value: selectedTarget.description,
            }
        ]}
    />;
}
