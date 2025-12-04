import { IEvaluatedReferenceLinksScore } from "../../../../taskViews/linking/linking.types";
import React from "react";
import {
    Tooltip,
    Markdown,
    ActivityControlWidget,
    ActivityControlWidgetProps,
    ActivityControlWidgetAction,
} from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { EvaluationScoreTooltip } from "./EvaluationScoreTooltip";

interface EvaluationActivityControlProps {
    score: IEvaluatedReferenceLinksScore | undefined;
    loading: boolean;
    referenceLinksUrl?: string;
    evaluationResultsShown?: boolean;
    evaluationResultsShownToggleButton?: ActivityControlWidgetAction;
    manualStartButton?: ActivityControlWidgetAction;
    ruleType?: "linking" | "transform";
}

/** Displays evaluation score and buttons to manually start evaluation for a rule. */
export const EvaluationActivityControl = ({
    score,
    loading,
    referenceLinksUrl,
    evaluationResultsShown,
    evaluationResultsShownToggleButton,
    manualStartButton,
    ruleType,
}: EvaluationActivityControlProps) => {
    const [t] = useTranslation();
    const isLinkingEvaluation = ruleType === "linking";

    const Menu = () => {
        const actionButtons = [] as ActivityControlWidgetAction[];
        if (isLinkingEvaluation && referenceLinksUrl) {
            actionButtons.push({
                "data-test-id": "open-reference-links-ui",
                icon: "item-edit",
                action: () => window.open(referenceLinksUrl, "_blank"),
                tooltip: t("RuleEditor.evaluation.scoreWidget.referenceLinks"),
            });
        }
        if (evaluationResultsShownToggleButton && !loading && (score || !!evaluationResultsShown)) {
            actionButtons.push(evaluationResultsShownToggleButton);
        }
        if (manualStartButton) {
            actionButtons.push(manualStartButton);
        }
        return actionButtons.length > 0 ? actionButtons : undefined;
    };

    let activityInfo = {
        label: <strong>{t("RuleEditor.evaluation.scoreWidget.title")}</strong>,
        statusMessage: t("RuleEditor.evaluation.scoreWidget.notStarted"),
    } as ActivityControlWidgetProps;
    let EvaluationTooltip = ({ children }: { children: React.JSX.Element }): React.JSX.Element => children;

    if (score) {
        activityInfo = {
            ...activityInfo,
            ...activityControlScoreProps(score),
        };
        EvaluationTooltip = ({ children }) => <EvaluationScoreTooltip score={score}>{children}</EvaluationScoreTooltip>;
    } else if (isLinkingEvaluation && !!evaluationResultsShown) {
        activityInfo = {
            ...activityInfo,
            statusMessage: t("RuleEditor.evaluation.scoreWidget.noScore"),
            progressBar: {
                animate: false,
                stripes: false,
                value: 0,
            },
        };
        EvaluationTooltip = ({ children }) => {
            return (
                <Tooltip
                    content={<Markdown>{t("RuleEditor.evaluation.scoreWidget.noScoreTooltip")}</Markdown>}
                    size={"large"}
                >
                    {children}
                </Tooltip>
            );
        };
    }

    if (loading) {
        activityInfo = {
            ...activityInfo,
            statusMessage: `${t("common.words.loading")}...`,
            progressSpinner: loading ? {} : undefined,
        };
    }

    return (
        <EvaluationTooltip>
            <ActivityControlWidget border small canShrink {...activityInfo} activityActions={Menu()} />
        </EvaluationTooltip>
    );
};

/** Generate activity control properties based on score. */
export const activityControlScoreProps = (score: IEvaluatedReferenceLinksScore): ActivityControlWidgetProps => {
    const fMeasure = Number.parseFloat(score.fMeasure);
    return {
        label: <strong>F / P / R</strong>,
        statusMessage: `${score.fMeasure} / ${score.precision} / ${score.recall}`,
        progressBar: {
            intent: "primary",
            animate: false,
            stripes: false,
            value: fMeasure,
        },
    };
};
