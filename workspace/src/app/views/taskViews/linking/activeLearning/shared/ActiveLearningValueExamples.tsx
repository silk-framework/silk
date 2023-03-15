import React from "react";
import { Highlighter, Tag, TagList } from "@eccenca/gui-elements";
import { colorValue } from "@eccenca/gui-elements/src/common/utils/colorDecideContrastvalue";
import { TagProps } from "@blueprintjs/core";

interface Props extends Pick<TagProps, "interactive"> {
    exampleValues: string[];
    valuesToHighlight?: Set<String>;
    highlightColor?: colorValue;
    onHover?: (val: string) => void;
    /** Search query that should be highlighted in the example values. */
    searchQuery?: string;
}

export const highlightedTagColor = "#745a85";

/** Shows example values for a property. */
export const ActiveLearningValueExamples = ({
    exampleValues,
    valuesToHighlight,
    highlightColor = "#745a85",
    onHover,
    searchQuery,
    ...otherTagProps
}: Props) => {
    const exampleTitle = exampleValues.join(" | ");

    return (
        <TagList
            className={
                "diapp-linking-learningdata__examples diapp-linking-learningdata__examples--count-" +
                (exampleValues.length > 3 ? "more" : exampleValues.length)
            }
        >
            {exampleValues.map((example, idx) => {
                const interactiveHoverProps = onHover
                    ? {
                          onMouseEnter: () => onHover(example),
                          onMouseLeave: () => onHover(""),
                      }
                    : {};
                const highlightValue: boolean = !!valuesToHighlight?.has(example);
                return (
                    <Tag
                        key={example + idx}
                        round={true}
                        htmlTitle={exampleTitle}
                        emphasis="stronger"
                        backgroundColor={highlightValue ? highlightColor : undefined}
                        {...interactiveHoverProps}
                        {...otherTagProps}
                    >
                        {searchQuery ? <Highlighter label={example} searchValue={searchQuery} /> : example}
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
