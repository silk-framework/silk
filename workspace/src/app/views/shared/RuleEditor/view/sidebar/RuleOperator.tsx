import { wrapTooltip } from "../../../../../utils/uiUtils";
import React from "react";
import {
    Icon,
    OverflowText,
    OverviewItemDescription,
    OverviewItemLine,
    Spacing,
    Highlighter,
    highlighterUtils,
    CLASSPREFIX as eccgui,
} from "@eccenca/gui-elements";
import utils from "../ruleNode/ruleNode.utils";
import { SidebarRuleOperatorBase } from "./RuleEditorOperatorSidebar.typings";
import Color from "color";
import getColorConfiguration from "@eccenca/gui-elements/src/common/utils/getColorConfiguration";
import { useTranslation } from "react-i18next";
import { RuleEditorUiContext } from "../../contexts/RuleEditorUiContext";

interface RuleOperatorProps {
    // The rule operator that should be rendered
    ruleOperator: SidebarRuleOperatorBase;
    // The original text query
    textQuery: string;
    // Multi-word search query
    searchWords: string[];
}

/** A single rule operator that is shown in the sidebar. */
export const RuleOperator = ({ ruleOperator, textQuery, searchWords }: RuleOperatorProps) => {
    const descriptionSearchSnippet =
        searchWords.length > 0 && ruleOperator.description
            ? extractSearchSnippet(ruleOperator.description, highlighterUtils.createMultiWordRegex(searchWords))
            : undefined;
    const itemLabel = ruleOperator.label;
    const [t] = useTranslation();
    const operatorDoc = ruleOperator.markdownDocumentation || ruleOperator.description || "";
    const ruleEditorUiContext = React.useContext(RuleEditorUiContext);

    return (
        <OverviewItemDescription>
            {wrapTooltip(
                itemLabel.length > 30,
                itemLabel,
                <OverviewItemLine>
                    <OverflowText ellipsis={"reverse"}>
                        <Highlighter label={itemLabel} searchValue={textQuery} />
                    </OverflowText>
                    {ruleOperator.description && !ruleOperator.markdownDocumentation && (
                        <>
                            <Spacing vertical={true} size={"tiny"} />
                            <Icon
                                name="item-info"
                                small
                                tooltipText={ruleOperator.description}
                                tooltipProps={{
                                    placement: "right",
                                    rootBoundary: "viewport",
                                }}
                            />
                        </>
                    )}
                    {ruleOperator.markdownDocumentation && (
                        <>
                            <Spacing vertical={true} size={"tiny"} />
                            <Icon
                                data-test-id="operator-markdown-icon"
                                name="item-question"
                                onClick={() =>
                                    ruleEditorUiContext.setCurrentRuleNodeInfo({
                                        description: operatorDoc,
                                        label: ruleOperator.label,
                                    })
                                }
                                small
                                tooltipText={t("RuleEditor.sidebar.operator.markdownTooltip", {
                                    shortDescription: ruleOperator.description,
                                })}
                                tooltipProps={{
                                    placement: "top",
                                    rootBoundary: "viewport",
                                }}
                            />
                        </>
                    )}
                </OverviewItemLine>,
                "bottom-end",
                "large",
            )}
            {descriptionSearchSnippet && (
                <OverviewItemLine data-test-id={"ruleOperator-sidebar-search-operator-description"}>
                    {wrapTooltip(
                        true,
                        ruleOperator.description!!,
                        <OverflowText>
                            <Highlighter label={descriptionSearchSnippet} searchValue={textQuery} />
                        </OverflowText>,
                        "bottom-end",
                        "medium",
                    )}
                </OverviewItemLine>
            )}
            <OverviewItemLine>
                {utils.createOperatorTags(
                    [...(ruleOperator.tags ?? []), ...(ruleOperator.categories ?? [])],
                    textQuery,
                    tagColor,
                )}
            </OverviewItemLine>
        </OverviewItemDescription>
    );
};

const tagColors = getColorConfiguration("react-flow-linking");
export const tagColor = (
    tag: "Input" | "Transform" | "Comparison" | "Aggregation" | "Source path" | "Target path",
): Color | string | undefined => {
    switch (tag) {
        case "Transform":
            return tagColors[`${eccgui}-transformation-node-bright`];
        case "Input":
            return tagColors[`${eccgui}-value-edge`];
        case "Comparison":
            return tagColors[`${eccgui}-comparator-node-bright`];
        case "Aggregation":
            return tagColors[`${eccgui}-aggregator-node-bright`];
        case "Source path":
            return tagColors[`${eccgui}-sourcepath-node-bright`];
        case "Target path":
            return tagColors[`${eccgui}-targetpath-node-bright`];
    }
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
