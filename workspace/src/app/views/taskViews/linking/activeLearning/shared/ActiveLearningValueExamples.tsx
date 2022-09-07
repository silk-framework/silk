import React, { CSSProperties } from "react";
import { Tag } from "@eccenca/gui-elements";

interface Props {
    exampleValues: string[];
    valuesToHighlight?: Set<String>;
    highlightStyle?: CSSProperties;
}

/** Shows example values for a property. */
export const ActiveLearningValueExamples = ({
    exampleValues,
    valuesToHighlight,
    highlightStyle = { backgroundColor: "lightgoldenrodyellow" },
}: Props) => {
    const exampleTitle = exampleValues.join(" | ");
    return (
        <span>
            {exampleValues.map((example, idx) => {
                const highlightValue: boolean = !!valuesToHighlight?.has(example);
                let style: CSSProperties = { marginRight: "0.25rem", backgroundColor: "white" };
                if (highlightValue) {
                    style = { ...style, ...highlightStyle };
                }
                return (
                    <Tag
                        key={example + idx}
                        small={true}
                        minimal={true}
                        round={true}
                        style={style}
                        htmlTitle={exampleTitle}
                    >
                        {example}
                    </Tag>
                );
            })}
        </span>
    );
};

/** Returns a set of the intersection of both string arrays. */
export const sameValues = (sourceValues: string[], targetValues: string[]): Set<string> => {
    const sourceSet = new Set(sourceValues);
    const resultSet = new Set<string>();
    targetValues.forEach((value) => sourceSet.has(value) && resultSet.add(value));
    return resultSet;
};
