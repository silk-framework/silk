import React from "react";
import { useTranslation } from "react-i18next";
import { Spacing, ToolbarSection, ContextOverlay, Icon } from "@eccenca/gui-elements";
import { ActivityControlWidget } from "@eccenca/gui-elements";
import { IntentTypes } from "@eccenca/gui-elements/src/common/Intent";
import { useTaskActivityWidget } from "../../shared/TaskActivityWidget/TaskActivityWidget";

interface LinkingRuleCacheInfoProps {
    /** Project ID the task is in. */
    projectId: string;
    /** The task the rules are being edited of. */
    taskId: string;
}

const getIntentState = (stateReference: string, statePath: string) => {
    // unique
    const mergedState = [stateReference, statePath].filter((x, i, a) => a.indexOf(x) === i);
    if (mergedState.length === 1) return mergedState[0];
    if (mergedState.includes("danger")) return "danger";
    if (mergedState.includes("warning")) return "warning";
    return "none";
};

export const LinkingRuleCacheInfo = ({ projectId, taskId }: LinkingRuleCacheInfoProps) => {
    const [t] = useTranslation();
    const [displayFullInfo, setDisplayFullInfo] = React.useState<boolean>(false);

    const referenceCache = useTaskActivityWidget({
        label: t("taskViews.linkRulesEditor.cacheWidgets.evaluationCache"),
        projectId: projectId,
        taskId: taskId,
        activityName: "ReferenceEntitiesCache",
        layoutConfig: { border: true, visualization: "spinner" },
        isCacheActivity: true,
    });

    const pathCache = useTaskActivityWidget({
        label: t("taskViews.linkRulesEditor.cacheWidgets.pathsCache"),
        projectId: projectId,
        taskId: taskId,
        activityName: "LinkingPathsCache",
        layoutConfig: { border: true, visualization: "spinner" },
        isCacheActivity: true,
    });

    const intent = getIntentState(referenceCache.intent, pathCache.intent);

    return (
        <>
            <ToolbarSection canShrink key={"pathsActivity"}>
                <ContextOverlay
                    isOpen={displayFullInfo}
                    onClose={() => {
                        setDisplayFullInfo(false);
                    }}
                    content={
                        <div style={{ width: "40rem", padding: "0.5rem" }}>
                            {referenceCache.widget}
                            <Spacing size="small" />
                            {pathCache.widget}
                        </div>
                    }
                >
                    <ActivityControlWidget
                        small
                        border
                        label={
                            <>
                                <strong>Caches: </strong>
                                {referenceCache.elapsedDateTime}
                                {" / "}
                                {pathCache.elapsedDateTime}
                            </>
                        }
                        progressSpinner={intent === "none" ? { intent, value: 0 } : undefined}
                        progressSpinnerFinishedIcon={
                            intent !== "none" ? (
                                <Icon name={[`state-${intent}`]} intent={intent as IntentTypes} />
                            ) : undefined
                        }
                        activityActions={[
                            {
                                icon: displayFullInfo ? "toggler-showless" : "toggler-showmore",
                                tooltip: "show cache info", // TODO translation
                                action: () => {
                                    setDisplayFullInfo(!displayFullInfo);
                                },
                            },
                        ]}
                    />
                </ContextOverlay>
            </ToolbarSection>
            <Spacing key={"spacing2"} vertical={true} size="small" />
        </>
    );
};
