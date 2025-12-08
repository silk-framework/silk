import React from "react";
import { Markdown, Tooltip } from "@eccenca/gui-elements";
import { IEvaluatedReferenceLinksScore } from "../../../../taskViews/linking/linking.types";
import { useTranslation } from "react-i18next";

interface EvaluationTooltipProps {
    children: React.JSX.Element;
    score?: IEvaluatedReferenceLinksScore;
}

/** Tooltip that displays the reference links score results. */
export const EvaluationScoreTooltip = ({ children, score }: EvaluationTooltipProps) => {
    const [t] = useTranslation();

    if (!score) {
        return children;
    }
    const allEvaluatedTrue = score.truePositives + score.falsePositives;
    const allTrue = score.truePositives + score.falseNegatives;
    const fMeasure = score.fMeasure.toString();
    const precision = score.precision.toString();
    const truePositives = score.truePositives.toString();
    const recall = score.recall.toString();
    const range = t("ReferenceLinks.evaluationScoreTooltipWidget.range");
    const fMeasureText = t("ReferenceLinks.evaluationScoreTooltipWidget.fMeasureText", { fMeasure, range });
    const precisionText = t("ReferenceLinks.evaluationScoreTooltipWidget.precisionText", {
        precision,
        range,
        truePositives,
        allEvaluatedTrue,
    });
    const recallText = t("ReferenceLinks.evaluationScoreTooltipWidget.recallText", {
        recall,
        range,
        truePositives,
        allTrue,
    });

    return (
        <Tooltip
            content={<Markdown allowHtml={true}>{fMeasureText + precisionText + recallText}</Markdown>}
            size={"large"}
        >
            {children}
        </Tooltip>
    );
};
