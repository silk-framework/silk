import React, { useContext } from "react";
import { HtmlContentBlock } from "@gui-elements/index";
import {ITargetWithSelected, SuggestionTypeValues} from "../../suggestion.typings";
import { SuggestionListContext } from "../../SuggestionContainer";
import { InfoBoxOverlay } from "./InfoBoxOverlay";

interface IProps {
    source?: string | ITargetWithSelected[];
    pathType?: SuggestionTypeValues
}

/** Shows additional information for a dataset source path, e.g. examples values. */
export function SourcePathInfoBox({source, pathType}: IProps) {
    const context = useContext(SuggestionListContext);
    const {exampleValues, portalContainer} = context;

    let examples: string[] | undefined = [];
    let sourcePath: string = ""
    if (typeof source === 'string') {
        sourcePath = source
        examples = exampleValues[source as string];
    } else if (Array.isArray(source)) {
        // There is always one item selected from the target list
        const selected = source.find(t => t._selected) as ITargetWithSelected;
        sourcePath = selected.uri
        if (selected && exampleValues[selected.uri]) {
            examples = exampleValues[selected.uri]
        }
    }

    const simpleStringValue = (str: string) => (<span style={{ wordBreak: "break-all" }}>{ str }</span>)

    const infoBoxProperties = [
        {
            key: "Source path",
            value: simpleStringValue(sourcePath)
        }
    ]

    if(examples && examples.length > 0) {
        infoBoxProperties.push({
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
        })
    }

    if(pathType) {
        infoBoxProperties.push({
            key: "Path type",
            value: simpleStringValue(pathType === "object" ? "Object path" : "Value path")
        })
    }

    return <InfoBoxOverlay
        data={infoBoxProperties}
    />;
}
