import React, { useContext } from "react";
import { HtmlContentBlock } from "@eccenca/gui-elements";
import {ITargetWithSelected, SuggestionTypeValues} from "../../suggestion.typings";
import { SuggestionListContext } from "../../SuggestionContainer";
import { InfoBoxOverlay } from "./InfoBoxOverlay";

interface IProps {
    source?: string | ITargetWithSelected[];
    pathType?: SuggestionTypeValues
    objectInfo?: {
        dataTypeSubPaths: string[]
        objectSubPaths: string[]
    }
}

/** Shows additional information for a dataset source path, e.g. examples values. */
export function SourcePathInfoBox({source, pathType, objectInfo}: IProps) {
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
                <InfoList items={Array.from(new Set(examples)).sort().slice(0, 9)} />
            </code>,
        })
    }

    if(pathType) {
        infoBoxProperties.push({
            key: "Path type",
            value: simpleStringValue(pathType === "object" ? "Object path" : "Value path")
        })
    }

    if(objectInfo) {
        infoBoxProperties.push({
            key: `Data type sub-paths (${objectInfo.dataTypeSubPaths.length})`,
            value: <div>
                <InfoList items={objectInfo.dataTypeSubPaths} />
            </div>
        })
        infoBoxProperties.push({
            key: `Object type sub-paths (${objectInfo.objectSubPaths.length})`,
            value: <div>
                <InfoList items={objectInfo.objectSubPaths} />
            </div>
        })
    }

    return <InfoBoxOverlay
        data={infoBoxProperties}
        data-test-id={"source-path-infobox"}
    />;
}

interface InfoListProps {
    items: string[]
}
const InfoList = ({items}: InfoListProps) => {
    return <HtmlContentBlock>
        <ul>
            {items.map((item) => {
                return <li key={item}>{item}</li>;
            })}
        </ul>
    </HtmlContentBlock>
}