import { OverflowText, Spacing, Tag } from "gui-elements";
import React from "react";
import { CLASSPREFIX as eccgui } from "gui-elements/src/configuration/constants";

const highlightedContainerClass = `${eccgui}-container--highlighted`;

interface LinkRuleNodeEvaluationProps {
    ruleOperatorId: string;
    /** Register for evaluation updates. */
    registerForEvaluationResults: (
        ruleOperatorId: string,
        evaluationUpdate: (evaluationValues: string[][] | undefined) => any
    ) => void;
    unregister: () => void;
}

/** Show linking evaluation results for a specific node. */
export const LinkRuleNodeEvaluation = ({
    ruleOperatorId,
    registerForEvaluationResults,
    unregister,
}: LinkRuleNodeEvaluationProps) => {
    const [evaluationResult, setEvaluationResult] = React.useState<string[][] | undefined>([]);

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
            ) : (
                <div>No evaluation example found</div>
            )}
            <Spacing size={"tiny"} />
        </div>
    ) : null;
};
