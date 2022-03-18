import { OverflowText, Spacing, Tag } from "gui-elements";
import React from "react";
import { CLASSPREFIX as eccgui } from "gui-elements/src/configuration/constants";
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
}

/** Show linking evaluation results for a specific node. */
export const LinkRuleNodeEvaluation = ({
    ruleOperatorId,
    registerForEvaluationResults,
    unregister,
    referenceLinksUrl,
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
        <div data-test-id={`evaluationNode${ruleOperatorId}`} style={{ backgroundColor: "#fefefe" }}>
            <Spacing size={"small"} />
            {evaluationResult.length > 0 ? (
                evaluationResult.map((rowValues, idx) => {
                    return (
                        <div key={idx}>
                            <OverflowText
                                className={`evaluationLink${idx}`}
                                onMouseEnter={() => onMouseEnter(idx)}
                                onMouseLeave={() => onMouseLeave(idx)}
                                title={rowValues.join(" | ")}
                            >
                                <Spacing size={"tiny"} vertical={true} />
                                {rowValues.map((value) => (
                                    <Tag small={true} minimal={true} round={true}>
                                        {value}
                                    </Tag>
                                ))}
                            </OverflowText>
                            {idx < evaluationResult?.length - 1 ? <Spacing size={"tiny"} /> : null}
                        </div>
                    );
                })
            ) : referenceLinksUrl ? (
                <div>
                    <Link href={referenceLinksUrl}>{t("RuleEditor.evaluation.noResults")}</Link>
                </div>
            ) : (
                <div>{t("RuleEditor.evaluation.noResults")}</div>
            )}
            <Spacing size={"tiny"} />
        </div>
    ) : null;
};
