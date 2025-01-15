import React from "react";
import { IPageSuggestion, ITargetWithSelected } from "../../suggestion.typings";
import { InfoBoxOverlay } from "./InfoBoxOverlay";
import { useSelector } from "react-redux";
import { commonSel } from "@ducks/common";

interface IProps {
    selectedTarget: ITargetWithSelected | IPageSuggestion;
}

export default function TargetInfoBox({ selectedTarget }: IProps) {
    const { dmBaseUrl } = useSelector(commonSel.initialSettingsSelector);
    const dmResourceLink = (graph: string, dmBaseUrl: string) => {
        return `${dmBaseUrl}/explore?resource=${encodeURIComponent(selectedTarget.uri)}&graph=${encodeURIComponent(
            graph
        )}`;
    };

    return (
        <InfoBoxOverlay
            data={[
                {
                    key: "Label",
                    value: selectedTarget.label,
                },
                {
                    key: "URI",
                    value: <span style={{ wordBreak: "break-all" }}>{selectedTarget.uri}</span>,
                },
                {
                    key: "Explore in DataManager",
                    value:
                        dmBaseUrl && selectedTarget.graph ? (
                            <a href={dmResourceLink(selectedTarget.graph, dmBaseUrl)} rel="noreferrer" target="_blank">
                                {selectedTarget.label}
                            </a>
                        ) : (
                            ""
                        ),
                },
                {
                    key: "Description",
                    value: selectedTarget.description,
                },
            ]}
        />
    );
}
