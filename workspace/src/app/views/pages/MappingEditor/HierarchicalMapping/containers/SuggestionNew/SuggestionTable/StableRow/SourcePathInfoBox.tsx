import React, { useContext } from "react";
import { HtmlContentBlock } from "@eccenca/gui-elements";
import {ITargetWithSelected, SuggestionTypeValues} from "../../suggestion.typings";
import { SuggestionListContext } from "../../SuggestionContainer";
import { InfoBoxOverlay } from "./InfoBoxOverlay";
import {useTranslation} from "react-i18next";

interface IProps {
    source?: string | ITargetWithSelected[];
    pathType?: SuggestionTypeValues
    objectInfo?: {
        dataTypeSubPaths: string[]
        objectSubPaths: string[]
    }
    embed?: boolean
    exampleValues?: string[]
}

/** Shows additional information for a dataset source path, e.g. examples values. */
export function SourcePathInfoBox({source, pathType, objectInfo, embed, exampleValues}: IProps) {
    const context = useContext(SuggestionListContext);
    const {exampleValues: contextExampleValues} = context;
    const [t] = useTranslation()

    let examples: string[] | undefined = [];
    let sourcePath: string = ""
    if(exampleValues) {
        examples = exampleValues
    } else if (typeof source === 'string') {
        sourcePath = source
        examples = contextExampleValues[source as string];
    } else if (Array.isArray(source)) {
        // There is always one item selected from the target list
        const selected = source.find(t => t._selected) as ITargetWithSelected;
        sourcePath = selected.uri
        if (selected && contextExampleValues[selected.uri]) {
            examples = contextExampleValues[selected.uri]
        }
    }

    const simpleStringValue = (str: string) => (<span style={{ wordBreak: "break-all" }}>{ str }</span>)

    const infoBoxProperties = [
        {
            key: t("MappingSuggestion.SourceElement.infobox.sourcePath"),
            value: simpleStringValue(sourcePath)
        }
    ]

    if(examples && examples.length > 0) {
        infoBoxProperties.push({
            key: t("MappingSuggestion.SourceElement.infobox.exampleData"),
            value: <code>
                <InfoList items={Array.from(new Set(examples)).sort().slice(0, 9)} />
            </code>,
        })
    }

    if(pathType) {
        infoBoxProperties.push({
            key: "Path type",
            value: simpleStringValue(pathType === "object" ?
                t("MappingSuggestion.SourceElement.infobox.objectPath") :
                t("MappingSuggestion.SourceElement.infobox.valuePath"))
        })
    }

    if(objectInfo) {
        infoBoxProperties.push({
            key: `${t("MappingSuggestion.SourceElement.infobox.dataTypeSubPaths")} (${objectInfo.dataTypeSubPaths.length})`,
            value: <div>
                <InfoList items={objectInfo.dataTypeSubPaths} />
            </div>
        })
        infoBoxProperties.push({
            key: `${t("MappingSuggestion.SourceElement.infobox.objectTypeSubPaths")} (${objectInfo.objectSubPaths.length})`,
            value: <div>
                <InfoList items={objectInfo.objectSubPaths} />
            </div>
        })
    }

    return <InfoBoxOverlay
        data={infoBoxProperties}
        data-test-id={"source-path-infobox"}
        embed={embed}
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
