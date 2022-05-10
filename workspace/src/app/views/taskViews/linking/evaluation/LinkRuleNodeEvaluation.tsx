import { WhiteSpaceContainer, Spacing, Tag, Tooltip } from "@eccenca/gui-elements";
import { NodeContentExtension } from "@eccenca/gui-elements/src/extensions/react-flow";
import React from "react";
import { CLASSPREFIX as eccgui } from "@eccenca/gui-elements/src/configuration/constants";
import { useTranslation } from "react-i18next";
import { Link } from "carbon-components-react";

const highlightedContainerClass = `${eccgui}-container--highlighted`;

interface LinkRuleNodeEvaluationProps {
    ruleOperatorId: string;
    /** Register for evaluation updates. */
    registerForEvaluationResults: (
        ruleOperatorId: string,
        evaluationUpdate: (evaluationValues: string[][] | undefined) => any
    ) => void;
    unregister: () => void;
    /** A URL to link to when there is no result found. */
    referenceLinksUrl?: string;
    numberOfLinksToShow: number;
}

/** Show linking evaluation results for a specific node. */
export const LinkRuleNodeEvaluation = ({
    ruleOperatorId,
    registerForEvaluationResults,
    unregister,
    referenceLinksUrl,
    numberOfLinksToShow,
}: LinkRuleNodeEvaluationProps) => {
    const [evaluationResult, setEvaluationResult] = React.useState<string[][] | undefined>([]);
    const [t] = useTranslation();

    React.useEffect(() => {
        registerForEvaluationResults(ruleOperatorId, setEvaluationResult);
        return unregister;
    }, []);

    const onMouseEnter = (lineIdx: number) => {
        const lines = document.querySelectorAll(`.evaluationLink${lineIdx}`);
        lines.forEach((element) => element.classList.add(highlightedContainerClass));
    };
    const onMouseLeave = (lineIdx: number) => {
        const lines = document.querySelectorAll(`.evaluationLink${lineIdx}`);
        lines.forEach((element) => element.classList.remove(highlightedContainerClass));
    };

    return evaluationResult ? (
        <NodeContentExtension isExpanded={true} data-test-id={`evaluationNode${ruleOperatorId}`}>
            {evaluationResult.length > 0 ? (
                <ul>
                    {evaluationResult.map((rowValues, idx) => {
                        return (
                            <li key={idx}>
                                <WhiteSpaceContainer
                                    className={`evaluationLink${idx}`}
                                    onMouseEnter={() => onMouseEnter(idx)}
                                    onMouseLeave={() => onMouseLeave(idx)}
                                    paddingTop="tiny"
                                    paddingBottom="tiny"
                                    style={{ whiteSpace: "nowrap", overflow: "hidden" }}
                                >
                                    <Tooltip
                                        content={rowValues.join(" | ")}
                                        tooltipProps={{
                                            placement: "top",
                                            boundary: "window",
                                            targetTagName: "div",
                                        }}
                                    >
                                        <span>
                                        {rowValues.map((value) => (
                                            <Tag
                                                small={true}
                                                minimal={true}
                                                round={true}
                                                style={{ marginRight: "0.25rem" }}
                                                htmlTitle={""}
                                            >
                                                {value}
                                            </Tag>
                                        ))}
                                        </span>
                                    </Tooltip>
                                </WhiteSpaceContainer>
                            </li>
                        );
                    })}
                </ul>
            ) : referenceLinksUrl ? (
                <div>
                    <Link href={referenceLinksUrl}>{t("RuleEditor.evaluation.noResults")}</Link>
                </div>
            ) : (
                <div>{t("RuleEditor.evaluation.noResults")}</div>
            )}
            {evaluationResult.length < numberOfLinksToShow && evaluationResult.length && referenceLinksUrl ? (
                <div>
                    <Spacing hasDivider={true} />
                    <Link href={referenceLinksUrl}>{t("RuleEditor.evaluation.addMoreResults")}</Link>
                </div>
            ) : null}
        </NodeContentExtension>
    ) : null;
};
