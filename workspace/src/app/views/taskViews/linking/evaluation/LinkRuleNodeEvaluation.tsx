import { OverflowText } from "gui-elements";
import React from "react";

interface LinkRuleNodeEvaluationProps {
    ruleOperatorId: string;
    /** Register for evaluation updates. */
    registerForEvaluationResults: (
        ruleOperatorId: string,
        evaluationUpdate: (evaluationValues: string[][]) => any
    ) => void;
    unregister: () => void;
}

/** Show linking evaluation results for a specific node. */
export const LinkRuleNodeEvaluation = ({
    ruleOperatorId,
    registerForEvaluationResults,
    unregister,
}: LinkRuleNodeEvaluationProps) => {
    const [evaluationResult, setEvaluationResult] = React.useState<string[][]>([]);

    React.useEffect(() => {
        registerForEvaluationResults(ruleOperatorId, setEvaluationResult);
        return unregister;
    }, []);

    return (
        <div data-test-id={`evaluationNode${ruleOperatorId}`}>
            {evaluationResult.length > 0 ? (
                evaluationResult.map((line) => {
                    return <OverflowText>{line}</OverflowText>;
                })
            ) : (
                <div>No evaluation for {ruleOperatorId}</div>
            )}
        </div>
    );
};
