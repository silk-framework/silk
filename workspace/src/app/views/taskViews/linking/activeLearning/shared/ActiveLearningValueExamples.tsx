import React from "react";
import { Tag, TagList } from "@eccenca/gui-elements";
import { colorValue } from "@eccenca/gui-elements/src/common/utils/colorDecideContrastvalue";

interface Props {
    exampleValues: string[];
    valuesToHighlight?: Set<String>;
    highlightColor?: colorValue;
}

export const highlightedTagColor = "#745a85"

/** Shows example values for a property. */
export const ActiveLearningValueExamples = ({
    exampleValues,
    valuesToHighlight,
    highlightColor = "#745a85",
}: Props) => {
    const exampleTitle = exampleValues.join(" | ");
    return (
        <TagList className={"diapp-linking-learningdata__examples diapp-linking-learningdata__examples--count-"+(exampleValues.length > 3 ? "more" : exampleValues.length)}>
            {exampleValues.map((example, idx) => {
                const highlightValue: boolean = !!valuesToHighlight?.has(example);
                return (
                    <Tag
                        key={example + idx}
                        round={true}
                        htmlTitle={exampleTitle}
                        backgroundColor={highlightValue ? highlightColor : undefined}
                    >
                        {example}
                    </Tag>
                );
            })}
        </TagList>
    );
};

/** Returns a set of the intersection of both string arrays. */
export const sameValues = (sourceValues: string[], targetValues: string[]): Set<string> => {
    const sourceSet = new Set(sourceValues);
    const resultSet = new Set<string>();
    targetValues.forEach((value) => sourceSet.has(value) && resultSet.add(value));
    return resultSet;
};
