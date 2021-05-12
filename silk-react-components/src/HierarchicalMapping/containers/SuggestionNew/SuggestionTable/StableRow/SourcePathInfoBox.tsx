import React, { useContext } from "react";
import { HtmlContentBlock } from "@gui-elements/index";
import { ITargetWithSelected } from "../../suggestion.typings";
import { SuggestionListContext } from "../../SuggestionContainer";
import { InfoBoxOverlay } from "./InfoBoxOverlay";

interface IProps {
    source?: string | ITargetWithSelected[];
}

/** Shows additional information for a dataset source path, e.g. examples values. */
export function SourcePathInfoBox({source}: IProps) {
    const context = useContext(SuggestionListContext);
    const {exampleValues, portalContainer} = context;

    let examples: string[] = [];
    let sourcePath = source
    if (typeof source === 'string') {
        examples = exampleValues[source as string];
    } else if (Array.isArray(source)) {
        // There is always one item selected from the target list
        const selected = source.find(t => t._selected) as ITargetWithSelected;
        sourcePath = selected.uri
        if (selected && exampleValues[selected.uri]) {
            examples = exampleValues[selected.uri]
        }
    }

    return <InfoBoxOverlay
        data={[
            {
              key: "Source path",
              value: (<span style={{ wordBreak: "break-all" }}>{ sourcePath }</span>)
            },
            {
                key: "Example data",
                value: <code>
                    <HtmlContentBlock>
                        <ul>
                            {Array.from(new Set(examples)).sort().slice(0, 9).map((item) => {
                                    return <li key={item}>{item}</li>;
                                })}
                        </ul>
                    </HtmlContentBlock>
                </code>,
            }
        ]}
    />;
}
