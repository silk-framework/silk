import { IViewActions } from "../../../plugins/PluginRegistry";
import { LinkingRuleActiveLearningContext } from "./contexts/LinkingRuleActiveLearningContext";
import React from "react";
import { LinkingRuleActiveLearningConfig } from "./LinkingRuleActiveLearningConfig";
import { fetchLinkSpec } from "../LinkingRuleEditor.requests";
import useErrorHandler from "../../../../hooks/useErrorHandler";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";
import { commonSel } from "@ducks/common";
import { TaskPlugin } from "@ducks/shared/typings";
import { ILinkingTaskParameters, ReferenceLinks } from "../linking.types";
import { ActivityAction, IActivityStatus, Spinner } from "@eccenca/gui-elements";
import { ActiveLearningStep, ComparisonPairWithId } from "./LinkingRuleActiveLearning.typings";
import { LinkingRuleActiveLearningMain } from "./learningUI/LinkingRuleActiveLearningMain";
import { LabelProperties } from "../referenceLinks/LinkingRuleReferenceLinks.typing";
import { activityActionCreator } from "../../../shared/TaskActivityOverview/taskActivityOverviewRequests";
import { DIErrorTypes } from "@ducks/error/typings";
import { connectWebSocket } from "../../../../services/websocketUtils";
import { activityQueryString } from "../../../shared/TaskActivityOverview/taskActivityUtils";
import { legacyApiEndpoint } from "../../../../utils/getApiEndpoint";
import { fetchActiveLearningReferenceLinks } from "./LinkingRuleActiveLearning.requests";

export interface LinkingRuleActiveLearningProps {
    /** Project ID the task is in. */
    projectId: string;
    /** The linking task for which to do the active learning. */
    linkingTaskId: string;
    /** Generic actions and callbacks on views. */
    viewActions?: IViewActions;
}

export const activeLearningActivities = {
    comparisonPairs: "ActiveLearning-ComparisonPairs",
};

/** Learns a linking rule via active learning.
 * The user configs pairs of properties that make sense to match.
 * The active learning algorithm presents the user with entity pair candidates that need to be either accepted or rejected,
 * building a set of links called reference links.
 * Based on this user-constructed gold standard the backend then learns the "optimal" linking rule which can then be saved.
 **/
export const LinkingRuleActiveLearning = ({ projectId, linkingTaskId }: LinkingRuleActiveLearningProps) => {
    const { registerError } = useErrorHandler();
    const [t] = useTranslation();
    const prefLang = useSelector(commonSel.localeSelector);
    const [taskData, setTaskData] = React.useState<TaskPlugin<ILinkingTaskParameters> | undefined>(undefined);
    const [loading, setLoading] = React.useState(false);
    const [selectedPropertyPairs, setSelectedPropertyPairs] = React.useState<ComparisonPairWithId[]>([]);
    const [activeLearningStep, setActiveLearningStep] = React.useState<ActiveLearningStep>("config");
    /** A copy of the reference links that can be modified. Changes to the backend will only be made when explicitly saving. */
    const [referenceLinks, setReferenceLinks] = React.useState<ReferenceLinks | undefined>(undefined);
    /** The source paths of the label values that should be displayed in the UI for each entity in a link. */
    const [labelPaths, setLabelPaths] = React.useState<LabelProperties | undefined>(undefined);
    const [comparisonPairsLoading, setComparisonPairsLoading] = React.useState(false);

    React.useEffect(() => {
        startComparisonPairActivity();
    }, [projectId, linkingTaskId]);

    const checkComparisonPairsActivityFinished = (
        activityStatus: IActivityStatus,
        unregisterFromUpdates: () => any
    ) => {
        let finished = true;
        switch (activityStatus.concreteStatus) {
            case "Successful":
                break;
            case "Failed":
                //TODO: Show warning to user in comparison pair widget
                break;
            case "Cancelled":
                // TODO: Show warning to user
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

    /** Fetches the current reference links of the linking task. */
    const fetchReferenceLinks = async (projectId: string, taskId: string) => {
        try {
            setLoading(true);
            const links = (await fetchActiveLearningReferenceLinks(projectId, taskId)).data;
            setReferenceLinks(links);
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
        await fetchReferenceLinks(projectId, linkingTaskId);
    };

    return (
        <LinkingRuleActiveLearningContext.Provider
            value={{
                projectId,
                linkingTaskId,
                linkTask: taskData,
                propertiesToCompare: selectedPropertyPairs,
                setPropertiesToCompare: setSelectedPropertyPairs,
                navigateTo: setActiveLearningStep,
                referenceLinks: referenceLinks,
                labelPaths,
                changeLabelPaths: setLabelPaths,
                comparisonPairsLoading,
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
                </>
            )}
        </LinkingRuleActiveLearningContext.Provider>
    );
};
