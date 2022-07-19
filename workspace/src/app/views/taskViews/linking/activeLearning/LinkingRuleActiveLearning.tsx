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
import { ILinkingTaskParameters } from "../linking.types";
import { Spinner } from "@eccenca/gui-elements";
import { ActiveLearningStep, CandidatePropertyPair } from "./LinkingRuleActiveLearning.typings";
import { LinkingRuleActiveLearningMain } from "./learningUI/LinkingRuleActiveLearningMain";
import { EntityLink } from "./learningUI/LinkingRuleActiveLearningMain.typings";

export interface LinkingRuleActiveLearningProps {
    /** Project ID the task is in. */
    projectId: string;
    /** The linking task for which to do the active learning. */
    linkingTaskId: string;
    /** Generic actions and callbacks on views. */
    viewActions?: IViewActions;
}

// TODO: remove when not needed anymore
const mockPairs: CandidatePropertyPair[] = [
    {
        pairId: "1",
        left: {
            value: "urn:prop:propA",
            label: "Property A",
            exampleValues: ["Value 1", "Value 2"],
            type: "number",
        },
        right: {
            value: "urn:prop:propB",
            label: "Property B",
            exampleValues: [
                "VeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongValue",
                "Next Value",
            ],
            type: "number",
        },
    },
    {
        pairId: "2",
        left: {
            value: "urn:prop:propA2",
            exampleValues: ["Value 1", "Value 2"],
            type: "number",
        },
        right: {
            value: "urn:prop:propB2",
            label: "urn:prop:propB",
            exampleValues: ["VeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongValue", "Next Value"],
            type: "number",
        },
    },
];

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
    const [selectedPropertyPairs, setSelectedPropertyPairs] = React.useState<CandidatePropertyPair[]>(mockPairs);
    const [activeLearningStep, setActiveLearningStep] = React.useState<ActiveLearningStep>("config");
    /** The original reference links. Used for diffing the reference links on save. */
    const referenceLinksInternal = React.useRef<EntityLink[]>([]);
    /** A copy of the reference links that can be modified. Changes to the backend will only be made when explicitly saving. */
    const [referenceLinks, setReferenceLinks] = React.useState<EntityLink[] | undefined>(undefined);

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
        const taskData = await fetchTaskData(projectId, linkingTaskId);
        if (taskData) {
            const referenceLinks = taskData.parameters.referenceLinks;
            setReferenceLinks(referenceLinks as any);
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
                navigateTo: setActiveLearningStep,
                referenceLinks: referenceLinks ?? [],
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
