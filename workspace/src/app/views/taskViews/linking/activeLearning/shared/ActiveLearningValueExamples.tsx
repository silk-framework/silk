import React from "react";
import { Highlighter, Tag, TagList, TagProps } from "@eccenca/gui-elements";
import { colorValue } from "@eccenca/gui-elements/src/common/utils/colorDecideContrastvalue";

interface Props extends Pick<TagProps, "interactive"> {
    exampleValues: string[];
    valuesToHighlight?: Set<String>;
    highlightColor?: colorValue;
    onHover?: (val: string) => void;
    /** Search query that should be highlighted in the example values. */
    searchQuery?: string;
    maxLength?: number;
}

export const highlightedTagColor = "#745a85";

/** Shows example values for a property. */
export const ActiveLearningValueExamples = ({
    exampleValues,
    valuesToHighlight,
    highlightColor = highlightedTagColor,
    onHover,
    searchQuery,
    maxLength = 3,
    ...otherTagProps
}: Props) => {
    const remainingExamples =
        maxLength && exampleValues.length > maxLength ? (
            <Tag
                className="diapp-linking-learningdata__examples__cutinfo"
                round
                intent="info"
                htmlTitle={exampleValues.slice(maxLength).join(" | ")}
            >
                +{exampleValues.length - maxLength}
            </Tag>
        ) : null;

    return (
        <TagList
            className={
                "diapp-linking-learningdata__examples diapp-linking-learningdata__examples--count-" +
                (exampleValues.length > (maxLength ?? 3) ? "more" : exampleValues.length)
            }
        >
            {exampleValues.slice(0, maxLength).map((example, idx) => {
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
                        htmlTitle={example}
                        emphasis="stronger"
                        backgroundColor={highlightValue ? highlightColor : undefined}
                        {...interactiveHoverProps}
                        {...otherTagProps}
                    >
                        {searchQuery ? <Highlighter label={example} searchValue={searchQuery} /> : example}
                    </Tag>
                );
            })}
            {remainingExamples}
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
