import {
    HtmlContentBlock,
    WhiteSpaceContainer,
    Spacing,
    Tag,
    Tooltip,
    Icon,
    OverflowText,
    OverviewItemLine,
    Link,
} from "@eccenca/gui-elements";
import { NodeContentExtension } from "@eccenca/gui-elements/src/extensions/react-flow";
import React from "react";
import { CLASSPREFIX as eccgui } from "@eccenca/gui-elements/src/configuration/constants";
import { useTranslation } from "react-i18next";
import { EvaluationResultType } from "./LinkingRuleEvaluation";
import {SampleError} from "../../../shared/SampleError/SampleError";

const highlightedContainerClass = `${eccgui}-container--highlighted`;

interface LinkRuleNodeEvaluationProps {
    ruleOperatorId: string;
    /** Register for evaluation updates. */
    registerForEvaluationResults: (
        ruleOperatorId: string,
        evaluationUpdate: (evaluationValues: EvaluationResultType | undefined) => void
    ) => void;
    unregister: () => void;
    /** A URL to link to when there is no result found. */
    referenceLinksUrl?: string;
    numberOfLinksToShow: number;
    noResultMsg?: string;
}

/** Show linking evaluation results for a specific node. */
export const LinkRuleNodeEvaluation = ({
    ruleOperatorId,
    registerForEvaluationResults,
    unregister,
    referenceLinksUrl,
    numberOfLinksToShow,
    noResultMsg,
}: LinkRuleNodeEvaluationProps) => {
    const [evaluationResult, setEvaluationResult] = React.useState<EvaluationResultType | undefined>([]);
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

    const EXAMPLES_MAX = 20;

    return evaluationResult ? (
        <NodeContentExtension isExpanded={true} data-test-id={`evaluationNode${ruleOperatorId}`}>
            {evaluationResult.length > 0 ? (
                <ul>
                    {evaluationResult.map((rowData, idx) => {
                        const { value, error } = rowData;
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
                                        content={
                                            error?.error ?? (
                                                <HtmlContentBlock>
                                                    {value.length === 1 && value[0]}
                                                    {value.length > 1 && (
                                                        <ul>
                                                            {value.slice(0, EXAMPLES_MAX - 1).map((exampleValue) => {
                                                                return (
                                                                    <li>
                                                                        <OverflowText>{exampleValue}</OverflowText>
                                                                    </li>
                                                                );
                                                            })}
                                                            {value.length > EXAMPLES_MAX && (
                                                                <li>+{value.length - EXAMPLES_MAX}</li>
                                                            )}
                                                        </ul>
                                                    )}
                                                </HtmlContentBlock>
                                            )
                                        }
                                        placement="top"
                                        rootBoundary="viewport"
                                        targetTagName="div"
                                        size="large"
                                    >
                                        <span>
                                            {error ? (
                                                <OverviewItemLine small>
                                                    <SampleError sampleError={error} hasStateWarning />
                                                    <Spacing size="tiny" vertical />
                                                    <OverflowText className="linking__error-description">
                                                        {error.error}
                                                    </OverflowText>
                                                </OverviewItemLine>
                                            ) : (
                                                value.slice(0, EXAMPLES_MAX - 1).map((value, i) => (
                                                    <Tag
                                                        key={i}
                                                        small={true}
                                                        minimal={true}
                                                        round={true}
                                                        style={{ marginRight: "0.25rem" }}
                                                        htmlTitle={""}
                                                    >
                                                        {value}
                                                    </Tag>
                                                ))
                                            )}
                                            {!error && value.length > EXAMPLES_MAX && (
                                                <Tag small={true} minimal={true} round={true} htmlTitle={""}>
                                                    +{value.length - EXAMPLES_MAX}
                                                </Tag>
                                            )}
                                        </span>
                                    </Tooltip>
                                </WhiteSpaceContainer>
                            </li>
                        );
                    })}
                </ul>
            ) : referenceLinksUrl ? (
                <div>
                    <Link href={referenceLinksUrl} target={"_blank"}>
                        {t("RuleEditor.evaluation.noResults")}
                    </Link>
                </div>
            ) : (
                <div>{noResultMsg ?? t("RuleEditor.evaluation.noResults")}</div>
            )}
            {evaluationResult.length < numberOfLinksToShow && evaluationResult.length && referenceLinksUrl ? (
                <div>
                    <Spacing hasDivider={true} />
                    <Link href={referenceLinksUrl} target={"_blank"}>
                        {t("RuleEditor.evaluation.addMoreResults")}
                    </Link>
                </div>
            ) : null}
        </NodeContentExtension>
    ) : null;
};
