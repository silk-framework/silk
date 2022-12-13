import { commonSel } from "@ducks/common";
import { requestRuleOperatorPluginDetails } from "@ducks/common/requests";
import { DIErrorTypes } from "@ducks/error/typings";
import { TaskPlugin } from "@ducks/shared/typings";
import { ActivityAction, IActivityStatus, Spinner } from "@eccenca/gui-elements";
import React from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import useErrorHandler from "../../../../hooks/useErrorHandler";
import { connectWebSocket } from "../../../../services/websocketUtils";
import { legacyApiEndpoint } from "../../../../utils/getApiEndpoint";
import ruleEditorUtils from "../../../../views/shared/RuleEditor/RuleEditor.utils";
import { useInitFrontend } from "../../../pages/MappingEditor/api/silkRestApi.hooks";
import { IViewActions } from "../../../plugins/PluginRegistry";
import { IRuleOperator, IRuleOperatorNode } from "../../../shared/RuleEditor/RuleEditor.typings";
import { activityActionCreator } from "../../../shared/TaskActivityOverview/taskActivityOverviewRequests";
import { activityQueryString } from "../../../shared/TaskActivityOverview/taskActivityUtils";
import ruleUtils from "../../shared/rules/rule.utils";
import { ILinkingRule, ILinkingTaskParameters } from "../linking.types";
import { fetchLinkSpec } from "../LinkingRuleEditor.requests";
import utils from "../LinkingRuleEditor.utils";
import { LabelProperties } from "../referenceLinks/LinkingRuleReferenceLinks.typing";
import { LinkingRuleActiveLearningContext } from "./contexts/LinkingRuleActiveLearningContext";
import { LinkingRuleActiveLearningResetModal } from "./dialogs/LinkingRuleActiveLearningResetModal";
import { SessionRunningWarningModal } from "./dialogs/SessionRunningWarningModal";
import { LinkingRuleActiveLearningMain } from "./learningUI/LinkingRuleActiveLearningMain";
import { ActiveLearningStep, ComparisonPairWithId } from "./LinkingRuleActiveLearning.typings";
import { LinkingRuleActiveLearningConfig } from "./LinkingRuleActiveLearningConfig";
import { useActiveLearningSessionInfo } from "./shared/ActiveLearningSessionInfoWidget";

export interface LinkingRuleActiveLearningProps {
    /** Project ID the task is in. */
    projectId: string;
    /** The linking task for which to do the active learning. */
    linkingTaskId: string;
    /** Generic actions and callbacks on views. */
    viewActions?: IViewActions;
}

export const activeLearningActivities = {
    activeLearning: "ActiveLearning",
    comparisonPairs: "ActiveLearning-ComparisonPairs",
};

/** Learns a linking rule via active learning.
 * The user configs pairs of properties that make sense to match.
 * The active learning algorithm presents the user with entity pair candidates that need to be either accepted or rejected,
 * building a set of links called reference links.
 * Based on this user-constructed gold standard the backend then learns the "optimal" linking rule which can then be saved.
 **/
export const LinkingRuleActiveLearning = ({
    projectId,
    linkingTaskId,
    viewActions,
}: LinkingRuleActiveLearningProps) => {
    const { registerError } = useErrorHandler();
    const [t] = useTranslation();
    const prefLang = useSelector(commonSel.localeSelector);
    const [taskData, setTaskData] = React.useState<TaskPlugin<ILinkingTaskParameters> | undefined>(undefined);
    const [loading, setLoading] = React.useState(true);
    const [selectedPropertyPairs, setSelectedPropertyPairs] = React.useState<ComparisonPairWithId[]>([]);
    const [activeLearningStep, setActiveLearningStep] = React.useState<ActiveLearningStep>("config");
    /** The source paths of the label values that should be displayed in the UI for each entity in a link. */
    const [labelPaths, setLabelPaths] = React.useState<LabelProperties | undefined>(undefined);
    const [comparisonPairsLoading, setComparisonPairsLoading] = React.useState(false);
    const [showResetDialog, setShowResetDialog] = React.useState(false);
    const [operatorMap, setOperatorMap] = React.useState<Map<string, IRuleOperator[]> | undefined>(undefined);
    // If an existing session exists and the current is new to this session, a modal will be displayed.
    const [showExistingSessionModal, setShowExistingSessionModal] = React.useState(false);
    // Basically tracks if a user has already clicked on the "Start learning" button
    const [activeLearningStarted, setActiveLearningStarted] = React.useState(false);
    const { sessionInfo } = useActiveLearningSessionInfo(projectId, linkingTaskId);
    const initData = useInitFrontend();

    React.useEffect(() => {
        startComparisonPairActivity();
    }, [projectId, linkingTaskId]);

    React.useEffect(() => {
        fetchRuleOperators();
    }, []);

    React.useEffect(() => {
        if (sessionInfo && initData && initData.userUri && sessionInfo.users.length > 0) {
            const sessionContainsUser = sessionInfo.users.find((user) => initData.userUri === user.uri);
            if (!sessionContainsUser) {
                // Session does not contain current user, display (warning) modal that a session is already running.
                setShowExistingSessionModal(true);
            }
        }
    }, [initData, sessionInfo]);

    const fetchRuleOperators = async () => {
        try {
            const operatorPlugins = Object.values((await requestRuleOperatorPluginDetails(false)).data);
            const operatorMap = new Map<string, IRuleOperator[]>();
            operatorPlugins.forEach((op) => operatorMap.set(op.pluginId, []));
            operatorPlugins.forEach((op) => {
                operatorMap.get(op.pluginId)!!.push(ruleUtils.convertRuleOperator(op, () => []));
            });
            setOperatorMap(operatorMap);
        } catch (ex) {
            registerError(
                "LinkingRuleActiveLearning.fetchRuleOperators",
                t("taskViews.linkRulesEditor.errors.fetchLinkingRuleOperatorDetails.msg"),
                ex
            );
        }
    };

    const convertRule = React.useCallback(
        (linkingRule: ILinkingRule): IRuleOperatorNode[] => {
            if (operatorMap) {
                const getOperatorNode = (pluginId: string, pluginType?: string): IRuleOperator | undefined => {
                    return ruleEditorUtils.getOperatorNode(pluginId, operatorMap, pluginType);
                };
                return utils.convertLinkingRuleToRuleOperatorNodes(linkingRule, getOperatorNode);
            } else {
                return [];
            }
        },
        [operatorMap]
    );

    const checkComparisonPairsActivityFinished = (
        activityStatus: IActivityStatus,
        unregisterFromUpdates: () => any
    ) => {
        let finished = true;
        switch (activityStatus.concreteStatus) {
            case "Successful":
                break;
            case "Failed":
                // Warning will be shown in the activity overview
                break;
            case "Cancelled":
                // do nothing
                break;
            default:
                // Wait
                finished = false;
        }
        if (finished) {
            // Close websocket connection and stop updates
            unregisterFromUpdates();
            setComparisonPairsLoading(false);
        }
    };

    const startComparisonPairActivity = async () => {
        setComparisonPairsLoading(true);
        const errorHandler = (activityName: string, action: ActivityAction, error: DIErrorTypes) => {
            registerError(
                "LinkingRuleActiveLearning.startComparisonPairActivity",
                "Could not start comparison pair finder activity.",
                error
            );
        };
        const executeActivity = activityActionCreator(
            activeLearningActivities.comparisonPairs,
            projectId,
            linkingTaskId,
            errorHandler
        );
        try {
            await executeActivity("start");
            const query = activityQueryString(projectId, linkingTaskId, activeLearningActivities.comparisonPairs);
            return connectWebSocket(
                legacyApiEndpoint(`/activities/updatesWebSocket${query}`),
                legacyApiEndpoint(`/activities/updates${query}`),
                checkComparisonPairsActivityFinished
            );
        } catch (ex) {
            errorHandler(activeLearningActivities.comparisonPairs, "start", ex);
            setComparisonPairsLoading(false);
        }
    };

    /** Fetches the parameters of the linking task */
    const fetchTaskData = async (projectId: string, taskId: string) => {
        try {
            setLoading(true);
            const taskData = (await fetchLinkSpec(projectId, taskId, true, prefLang)).data;
            setTaskData(taskData);
            return taskData;
        } catch (err) {
            registerError(
                "LinkingRuleEditor_fetchLinkingTask",
                t("taskViews.linkRulesEditor.errors.fetchTaskData.msg"),
                err
            );
        } finally {
            setLoading(false);
        }
    };

    /** Load linking task data. */
    React.useEffect(() => {
        init();
    }, [projectId, linkingTaskId]);

    const init = async () => {
        await fetchTaskData(projectId, linkingTaskId);
    };

    const navigate = (toStep: ActiveLearningStep | "linkingEditor") => {
        let navigateTo: ActiveLearningStep = "config";
        if (toStep === "linkingEditor") {
            if (viewActions?.integratedView || !viewActions?.switchToView) {
                // Cannot navigate away from the current view in integrated mode.
                navigateTo = "config";
            } else {
                // FIXME: Change view index to view ID when possible
                viewActions.switchToView(0);
            }
        } else {
            navigateTo = toStep;
        }
        setActiveLearningStep(navigateTo);
        if (navigateTo === "linkLearning") {
            setActiveLearningStarted(true);
        }
    };

    const onCloseResetModal = (hasReset: boolean) => {
        setShowResetDialog(false);
        if (hasReset) {
            setActiveLearningStarted(false);
            setActiveLearningStep("config");
            setSelectedPropertyPairs([]);
            setLabelPaths(undefined);
            startComparisonPairActivity();
            init();
        }
    };

    return (
        <LinkingRuleActiveLearningContext.Provider
            value={{
                projectId,
                linkingTaskId,
                linkTask: taskData,
                propertiesToCompare: selectedPropertyPairs,
                setPropertiesToCompare: setSelectedPropertyPairs,
                navigateTo: navigate,
                labelPaths,
                changeLabelPaths: setLabelPaths,
                comparisonPairsLoading,
                showResetDialog: () => setShowResetDialog(true),
                convertRule,
                learningStarted: activeLearningStarted,
            }}
        >
            {loading ? (
                <Spinner />
            ) : (
                <>
                    {activeLearningStep === "config" ? (
                        <LinkingRuleActiveLearningConfig projectId={projectId} linkingTaskId={linkingTaskId} />
                    ) : null}
                    {activeLearningStep === "linkLearning" ? (
                        <LinkingRuleActiveLearningMain projectId={projectId} linkingTaskId={linkingTaskId} />
                    ) : null}
                    {showResetDialog ? <LinkingRuleActiveLearningResetModal close={onCloseResetModal} /> : null}
                </>
            )}
            {showExistingSessionModal ? (
                <SessionRunningWarningModal
                    close={() => setShowExistingSessionModal(false)}
                    activeLearningSessionInfo={sessionInfo}
                />
            ) : null}
        </LinkingRuleActiveLearningContext.Provider>
    );
};
