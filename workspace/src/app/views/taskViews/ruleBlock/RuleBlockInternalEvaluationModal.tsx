import React from "react";
import { IconButton, Notification, modalPreventEvents } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { IRuleBlockInputExample, RuleBlockSnapshot } from "./ruleBlock.types";
import { EvaluatedTransformEntity } from "../transform/transform.types";
import { RuleBlockEditor, RuleBlockEditorOptionalContext } from "./RuleBlockEditor";
import { RuleBlockEvaluationOptionalContext } from "./RuleBlockEvaluationOptionalContext";
import { requestRuleBlockEvaluation } from "./ruleBlock.requests";
import Loading from "../../shared/Loading";
import { RuleEditorBaseModal } from "../../shared/RuleEditor/view/components/RuleEditorBaseModal";

interface RuleBlockInternalEvaluationModalProps {
    projectId: string;
    ruleBlockId: string;
    snapshot: RuleBlockSnapshot;
    inputExamples: IRuleBlockInputExample[];
    ruleBlockLabel?: string;
    onClose: () => void;
}

/** Shows the internal rule block evaluation inside a transform/linking rule editor evaluation. */
export const RuleBlockInternalEvaluationModal = ({
    projectId,
    ruleBlockId,
    snapshot,
    inputExamples,
    ruleBlockLabel,
    onClose,
}: RuleBlockInternalEvaluationModalProps) => {
    const [t] = useTranslation();
    const [fullScreen, setFullScreen] = React.useState(false);
    const [evaluationResults, setEvaluationResults] = React.useState<EvaluatedTransformEntity[] | undefined>(undefined);
    const [evaluationError, setEvaluationError] = React.useState<string | undefined>(undefined);
    const modalWrapperEventHandlers = React.useMemo(
        () => ({
            ...modalPreventEvents,
            // Allow mouseup to reach the embedded rule editor, e.g. when completing react-flow connections.
            onMouseUp: () => {},
        }),
        [],
    );
    const ruleBlockDisplayLabel = ruleBlockLabel || t("common.dataTypes.ruleblock");
    const title = t("taskViews.ruleBlock.internalEvaluation.title", {
        label: ruleBlockDisplayLabel,
        defaultValue: `Evaluation of ${ruleBlockDisplayLabel}`,
    });

    React.useEffect(() => {
        let active = true;
        setEvaluationResults(undefined);
        setEvaluationError(undefined);
        void requestRuleBlockEvaluation(projectId, ruleBlockId, {
            ports: snapshot.ports,
            inputExamples,
            operatorTree: snapshot.operatorTree,
            layout: snapshot.layout,
            uiAnnotations: snapshot.uiAnnotations,
        })
            .then((result) => {
                if (active) {
                    setEvaluationResults(result.data ?? []);
                }
            })
            .catch((error) => {
                if (active) {
                    const errorMessage = error?.message
                        ? `${t("taskViews.ruleBlock.errors.evaluate")}: ${error.message}`
                        : t("taskViews.ruleBlock.errors.evaluate");
                    setEvaluationError(errorMessage);
                }
            });
        return () => {
            active = false;
        };
    }, [
        inputExamples,
        projectId,
        ruleBlockId,
        snapshot.layout,
        snapshot.operatorTree,
        snapshot.ports,
        snapshot.uiAnnotations,
        t,
    ]);

    return (
        <RuleEditorBaseModal
            data-test-id="rule-block-internal-evaluation-modal"
            onClose={onClose}
            title={title}
            isOpen={true}
            size={fullScreen ? "fullscreen" : "xlarge"}
            preventReactFlowEvents={false}
            wrapperDivProps={modalWrapperEventHandlers}
            headerOptions={[
                <IconButton
                    key="toggle-rule-block-internal-evaluation-size"
                    onClick={() => setFullScreen((current) => !current)}
                    data-test-id="toggle-fullscreen-btn"
                    text={
                        fullScreen
                            ? t("common.action.minimize", { defaultValue: "Minimize" })
                            : t("common.action.maximize", { defaultValue: "Maximize" })
                    }
                    name={fullScreen ? "toggler-minimize" : "toggler-maximize"}
                />,
                <IconButton
                    key="close-rule-block-internal-evaluation"
                    onClick={onClose}
                    data-test-id="close-btn"
                    text={t("common.action.close")}
                    name="navigation-close"
                />,
            ]}
        >
            <RuleBlockEditorOptionalContext.Provider
                value={{
                    showRuleOnly: true,
                    readOnly: true,
                    ruleBlockSnapshot: snapshot,
                    inputExamples,
                    ruleBlockLabel,
                }}
            >
                <RuleBlockEvaluationOptionalContext.Provider value={{ externalEvaluationResults: evaluationResults }}>
                    {evaluationError ? (
                        <Notification intent="danger">{evaluationError}</Notification>
                    ) : evaluationResults !== undefined ? (
                        <div style={{ position: "relative", height: "100%" }}>
                            <RuleBlockEditor
                                projectId={projectId}
                                ruleBlockTaskId={ruleBlockId}
                                instanceId={`rule-block-internal-evaluation-${ruleBlockId}`}
                            />
                        </div>
                    ) : (
                        <Loading />
                    )}
                </RuleBlockEvaluationOptionalContext.Provider>
            </RuleBlockEditorOptionalContext.Provider>
        </RuleEditorBaseModal>
    );
};
