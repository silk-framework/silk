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

interface EvaluationActivityControlProps {
    score: IEvaluatedReferenceLinksScore | undefined;
    loading: boolean;
    referenceLinksUrl?: string;
    evaluationResultsShown?: boolean;
    evaluationResultsShownToggleButton?: ActivityControlWidgetAction;
    manualStartButton?: ActivityControlWidgetAction;
}

/** Displays evaluation score and buttons to manually start evaluation for a rule. */
export const EvaluationActivityControl = ({
    score,
    loading,
    referenceLinksUrl,
    evaluationResultsShown,
    evaluationResultsShownToggleButton,
    manualStartButton,
}: EvaluationActivityControlProps) => {
    const [t] = useTranslation();

    const Menu = () => {
        const actionButtons = [] as ActivityControlWidgetAction[];
        if (referenceLinksUrl) {
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
    let EvaluationTooltip = ({ children }: { children: JSX.Element }): JSX.Element => children;

    if (score) {
        const allEvaluatedTrue = score.truePositives + score.falsePositives;
        const allTrue = score.truePositives + score.falseNegatives;
        const fMeasure = Number.parseFloat(score.fMeasure);
        const range = "Ranging from 0.0 (worst) to 1.0 (best).";
        const fmeasureText = `<h3>F1-measure: ${score.fMeasure}</h3><p>This is the combination of precision and recall. ${range}</p>`;
        const precisionText = `<h3>Precision: ${score.precision}</h3><p>How precise the rule is, i.e. the ratio of correctly evaluated positive items (${score.truePositives}) vs. all positively evaluated items (${allEvaluatedTrue}). ${range}</p>`;
        const recallText = `<h3>Recall: ${score.recall}</h3><p>Specifies how many of all the positive items are categorized correctly, i.e the ratio of correctly evaluated positive items (${score.truePositives}) vs all existing positive items (${allTrue}). ${range}</p>`;

        activityInfo = {
            ...activityInfo,
            label: <strong>F / P / R</strong>,
            statusMessage: `${score.fMeasure} / ${score.precision} / ${score.recall}`,
            progressBar: {
                intent: "primary",
                animate: false,
                stripes: false,
                value: fMeasure,
            },
        };
        EvaluationTooltip = ({ children }) => (
            <Tooltip
                content={<Markdown allowHtml={true}>{fmeasureText + precisionText + recallText}</Markdown>}
                size={"large"}
            >
                {children}
            </Tooltip>
        );
    } else if (!!evaluationResultsShown) {
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
            progressSpinner: loading ? { intent: "none" } : undefined,
        };
    }

    return (
        <EvaluationTooltip>
            <ActivityControlWidget border small canShrink {...activityInfo} activityActions={Menu()} />
        </EvaluationTooltip>
    );
};
