import { WhiteSpaceContainer, Tag, Tooltip } from "@eccenca/gui-elements";
import { NodeContentExtension } from "@eccenca/gui-elements/src/extensions/react-flow";
import React from "react";
import { CLASSPREFIX as eccgui } from "@eccenca/gui-elements/src/configuration/constants";

const highlightedContainerClass = `${eccgui}-container--highlighted`;

interface TransformRuleNodeEvaluationProps {
    ruleOperatorId: string;
    /** Register for evaluation updates. */
    registerForEvaluationResults: (
        ruleOperatorId: string,
        evaluationUpdate: (evaluationValues: string[][] | undefined) => any
    ) => void;
    unregister: () => void;
    /** A URL to link to when there is no result found. */
    numberOfLinksToShow: number;
    noResultMsg?: string;
}

/** Show linking evaluation results for a specific node. */
export const TransformRuleNodeEvaluation = ({
    ruleOperatorId,
    registerForEvaluationResults,
    unregister,
    numberOfLinksToShow,
    noResultMsg,
}: TransformRuleNodeEvaluationProps) => {
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
                                        placement="top"
                                        rootBoundary="viewport"
                                        targetTagName="div"
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
            ) : null}
        </NodeContentExtension>
    ) : null;
};
