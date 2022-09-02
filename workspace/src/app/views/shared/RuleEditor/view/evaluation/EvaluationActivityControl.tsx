import { IEvaluatedReferenceLinksScore } from "../../../../taskViews/linking/linking.types";
import React from "react";
import { Tooltip } from "@eccenca/gui-elements";
import { Markdown, ActivityControlWidget } from "@eccenca/gui-elements";
import {
    IActivityControlProps,
    IActivityAction,
} from "@eccenca/gui-elements/src/cmem/ActivityControl/ActivityControlWidget";
import { useTranslation } from "react-i18next";

interface EvaluationActivityControlProps {
    score: IEvaluatedReferenceLinksScore | undefined;
    loading: boolean;
    referenceLinksUrl?: string;
    evaluationResultsShown?: boolean;
    manualStartButton?: IActivityAction;
}

/** Displays evaluation score and buttons to manually start evaluation for a rule. */
export const EvaluationActivityControl = ({
    score,
    loading,
    referenceLinksUrl,
    evaluationResultsShown,
    manualStartButton,
}: EvaluationActivityControlProps) => {
    const [t] = useTranslation();

    const Menu = () => {
        const actionButtons = [] as IActivityAction[];
        if (referenceLinksUrl) {
            actionButtons.push({
                "data-test-id": "open-reference-links-ui",
                icon: "item-edit",
                action: () => window.open(referenceLinksUrl, "_blank"),
                tooltip: t("RuleEditor.evaluation.scoreWidget.referenceLinks"),
            });
        }
        if (manualStartButton) {
            actionButtons.push(manualStartButton);
        }
        return actionButtons.length > 0 ? actionButtons : undefined;
    };

    let activityInfo = {} as IActivityControlProps;

    if (!!evaluationResultsShown) {
        if (score) {
            const allEvaluatedTrue = score.truePositives + score.falsePositives;
            const allTrue = score.truePositives + score.falseNegatives;
            const fMeasure = Number.parseFloat(score.fMeasure);
            const range = "Ranging from 0.0 (worst) to 1.0 (best).";
            const fmeasureText = `<h3>F1-measure: ${score.fMeasure}</h3><p>This is the combination of precision and recall. ${range}</p>`;
            const precisionText = `<h3>Precision: ${score.precision}</h3><p>How precise the rule is, i.e. the ratio of correctly evaluated positive items (${score.truePositives}) vs. all positively evaluated items (${allEvaluatedTrue}). ${range}</p>`;
            const recallText = `<h3>Recall: ${score.recall}</h3><p>Specifies how many of all the positive items are categorized correctly, i.e the ratio of correctly evaluated positive items (${score.truePositives}) vs all existing positive items (${allTrue}). ${range}</p>`;

            activityInfo = {
                label: (
                    <Tooltip
                        content={<Markdown allowHtml={true}>{fmeasureText + precisionText + recallText}</Markdown>}
                        size={"large"}
                    >
                        <strong>F / P / R</strong>
                    </Tooltip>
                ),
                statusMessage: loading ? "loading" : `${score.fMeasure} / ${score.precision} / ${score.recall}`,
                progressBar: {
                    intent: "primary",
                    animate: false,
                    stripes: false,
                    value: fMeasure,
                },
                progressSpinner: loading ? { intent: "none" } : undefined,
            };
        } else {
            activityInfo = {
                label: (
                    <Tooltip
                        content={<Markdown>{t("RuleEditor.evaluation.scoreWidget.noScoreTooltip")}</Markdown>}
                        size={"large"}
                    >
                        {t("RuleEditor.evaluation.scoreWidget.noScore")}
                    </Tooltip>
                ),
                progressBar: {
                    animate: false,
                    stripes: false,
                    value: 0,
                },
            };
        }
    }

    return (
        <ActivityControlWidget
            border={evaluationResultsShown}
            small
            canShrink
            {...activityInfo}
            activityActions={Menu()}
        />
    );
};
