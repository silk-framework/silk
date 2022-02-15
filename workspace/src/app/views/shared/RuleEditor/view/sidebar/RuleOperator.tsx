import { wrapTooltip } from "../../../../../utils/uiUtils";
import React from "react";
import { IRuleOperator } from "../../RuleEditor.typings";
import Highlighter, { createMultiWordRegex } from "gui-elements/src/components/Typography/Highlighter";
import { Icon, OverflowText, OverviewItemDescription, OverviewItemLine, Spacing } from "gui-elements";
import utils from "../ruleNode/ruleNode.utils";

interface RuleOperatorProps {
    // The rule operator that should be rendered
    ruleOperator: IRuleOperator;
    // The original text query
    textQuery: string;
    // Multi-word search query
    searchWords: string[];
}

/** A single rule operator that is shown in the sidebar. */
export const RuleOperator = ({ ruleOperator, textQuery, searchWords }: RuleOperatorProps) => {
    const descriptionSearchSnippet =
        searchWords.length > 0 && ruleOperator.description
            ? extractSearchSnippet(ruleOperator.description, createMultiWordRegex(searchWords))
            : undefined;
    const itemLabel = ruleOperator.label;
    return (
        <OverviewItemDescription>
            {wrapTooltip(
                itemLabel.length > 30,
                itemLabel,
                <OverviewItemLine>
                    {/** TODO: CMEM-3917: add icon*/}
                    <Spacing vertical={true} size={"tiny"} />
                    <OverflowText>
                        <Highlighter label={itemLabel} searchValue={textQuery} />
                    </OverflowText>
                    {ruleOperator.description && <Icon name="item-info" small tooltipText={ruleOperator.description} />}
                </OverviewItemLine>,
                "bottom-right",
                "medium"
            )}
            {descriptionSearchSnippet && (
                <OverviewItemLine data-test-id={"ruleOperator-sidebar-search-operator-description"}>
                    {wrapTooltip(
                        true,
                        ruleOperator.description!!,
                        <OverflowText>
                            <Highlighter label={descriptionSearchSnippet} searchValue={textQuery} />
                        </OverflowText>,
                        "bottom-right",
                        "medium"
                    )}
                </OverviewItemLine>
            )}
            <OverviewItemLine>
                {utils.createOperatorTags([ruleOperator.pluginType, ...(ruleOperator.categories ?? [])], textQuery)}
            </OverviewItemLine>
        </OverviewItemDescription>
    );
};

// Returns the text starting around the first matching word from the query. This is used to show the first matching snippet of a longer text.
const extractSearchSnippet = (text: string, multiWordRegex: RegExp): string | undefined => {
    const matchResult = text ? multiWordRegex.exec(text) : undefined;
    if (matchResult) {
        const prefix = text.substring(0, matchResult.index);
        let wordAlignedIdx = matchResult.index;
        // Search for beginning of word with the matching substring, so the snippet does not start with gibberish
        for (let i = prefix.length - 1; i >= 0 && !whiteSpaceRegex.test(text[i]); i--) {
            wordAlignedIdx = i;
        }
        return text.substring(wordAlignedIdx);
    }
};

const whiteSpaceRegex = /\s+/;
