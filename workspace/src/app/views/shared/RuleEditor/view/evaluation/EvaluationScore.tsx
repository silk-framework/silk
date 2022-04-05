import { IEvaluatedReferenceLinksScore } from "../../../../taskViews/linking/linking.types";
import React from "react";
import {
    IconButton,
    OverviewItem,
    OverviewItemActions,
    OverviewItemDescription,
    OverviewItemLine,
    ProgressBar,
    Spacing,
    Spinner,
    Tooltip,
} from "gui-elements";
import { Markdown } from "gui-elements/cmem";
import { useTranslation } from "react-i18next";

interface EvaluationScoreProps {
    score: IEvaluatedReferenceLinksScore | undefined;
    loading: boolean;
    referenceLinksUrl?: string;
}

/** Displays the evaluation score for a rule. */
export const EvaluationScore = ({ score, loading, referenceLinksUrl }: EvaluationScoreProps) => {
    const [t] = useTranslation();

    const Menu = () => {
        return referenceLinksUrl ? (
            <IconButton
                data-test-id={"open-reference-links-ui"}
                name="item-edit"
                text={t("RuleEditor.evaluation.scoreWidget.referenceLinks")}
                href={referenceLinksUrl}
                target="_blank"
            />
        ) : null;
    };

    if (score) {
        const allEvaluatedTrue = score.truePositives + score.falsePositives;
        const allTrue = score.truePositives + score.falseNegatives;
        const fMeasure = Number.parseFloat(score.fMeasure);
        const range = "Ranging from 0.0 (worst) to 1.0 (best).";
        const precisionText = `<h3>Precision: ${score.precision}</h3><p>How precise the rule is, i.e. the ratio of correctly evaluated positive items (${score.truePositives}) vs. all positively evaluated items (${allEvaluatedTrue}). ${range}</p>`;
        const recallText = `<h3>Recall: ${score.recall}</h3><p>Specifies how many of all the positive items are categorized correctly, i.e the ratio of correctly evaluated positive items (${score.truePositives}) vs all existing positive items (${allTrue}). ${range}</p>`;
        return (
            <OverviewItem>
                <OverviewItemDescription>
                    <OverviewItemLine>
                        <Tooltip content={"F1-measure: combination of precision and recall. " + range}>
                            <div>
                                F-measure: {loading ? <Spinner size={"tiny"} position={"inline"} /> : score.fMeasure}
                            </div>
                        </Tooltip>
                    </OverviewItemLine>
                    <OverviewItemLine>
                        <Tooltip
                            content={<Markdown allowHtml={true}>{precisionText + recallText}</Markdown>}
                            size={"large"}
                        >
                            {loading ? (
                                "PR: loading..."
                            ) : (
                                <div>
                                    PR: {score.precision}
                                    <Spacing vertical hasDivider />
                                    {score.recall}
                                </div>
                            )}
                        </Tooltip>
                    </OverviewItemLine>
                    <OverviewItemLine>
                        <ProgressBar intent={"primary"} animate={false} stripes={false} value={fMeasure} />
                    </OverviewItemLine>
                </OverviewItemDescription>
                {referenceLinksUrl ? (
                    <OverviewItemActions>
                        <Menu />
                    </OverviewItemActions>
                ) : null}
            </OverviewItem>
        );
    } else {
        return (
            <OverviewItem>
                <OverviewItemDescription>
                    <OverviewItemLine small={true}>{t("RuleEditor.evaluation.scoreWidget.noResult")}</OverviewItemLine>
                    <OverviewItemLine>
                        <ProgressBar animate={false} stripes={false} value={0} />
                    </OverviewItemLine>
                </OverviewItemDescription>
                {referenceLinksUrl ? (
                    <OverviewItemActions>
                        <Menu />
                    </OverviewItemActions>
                ) : null}
            </OverviewItem>
        );
    }
};
