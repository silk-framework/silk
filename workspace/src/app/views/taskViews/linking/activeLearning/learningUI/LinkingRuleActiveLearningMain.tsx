import React from "react";
import { Button, IActivityStatus, Spacing, Toolbar, ToolbarSection } from "@eccenca/gui-elements";
import { LinkingRuleActiveLearningContext } from "../contexts/LinkingRuleActiveLearningContext";
import { LinkingRuleActiveLearningFeedbackComponent } from "./LinkingRuleActiveLearningFeedbackComponent";
import { EntityLink } from "../../referenceLinks/LinkingRuleReferenceLinks.typing";
import { LinkingRuleActiveLearningFeedbackContext } from "../contexts/LinkingRuleActiveLearningFeedbackContext";
import { LinkingRuleActiveLearningBestLearnedRule } from "./LinkingRuleActiveLearningBestLearnedRule";
import { LinkingRuleReferenceLinks } from "../../referenceLinks/LinkingRuleReferenceLinks";
import {
    fetchActiveLearningReferenceLinks,
    nextActiveLearningLinkCandidate,
    submitActiveLearningReferenceLink,
} from "../LinkingRuleActiveLearning.requests";
import referenceLinksUtils from "../../referenceLinks/LinkingRuleReferenceLinks.utils";
import { ActiveLearningDecisions } from "../LinkingRuleActiveLearning.typings";
import { ReferenceLinks } from "../../linking.types";
import useErrorHandler from "../../../../../hooks/useErrorHandler";
import { useTranslation } from "react-i18next";
import { activityQueryString } from "../../../../shared/TaskActivityOverview/taskActivityUtils";
import { connectWebSocket } from "../../../../../services/websocketUtils";
import { legacyApiEndpoint } from "../../../../../utils/getApiEndpoint";
import { activeLearningActivities } from "../LinkingRuleActiveLearning";

interface LinkingRuleActiveLearningMainProps {
    projectId: string;
    linkingTaskId: string;
}

/**
 * The main step of the active learning process that generates a gold standard through active learning
 * and learns a linking rule.
 */
export const LinkingRuleActiveLearningMain = ({ projectId, linkingTaskId }: LinkingRuleActiveLearningMainProps) => {
    const [t] = useTranslation();
    const activeLearningContext = React.useContext(LinkingRuleActiveLearningContext);
    const [loadingLinkCandidate, setLoadingLinkCandidate] = React.useState(false);
    /** The currently selected entity, either an unlabeled link or an existing reference link. */
    const [selectedEntityLink, setSelectedEntityLink] = React.useState<EntityLink | undefined>(undefined);
    /** The list of reference links. */
    const [referenceLinks, setReferenceLinks] = React.useState<ReferenceLinks | undefined>(undefined);
    const [referenceLinksLoading, setReferenceLinksLoading] = React.useState(false);
    const { registerError } = useErrorHandler();

    React.useEffect(() => {
        if (!selectedEntityLink) {
            loadUnlabeledLinkCandidate();
        }
    }, [selectedEntityLink]);

    React.useEffect(() => {
        fetchReferenceLinks(projectId, linkingTaskId);
        const query = activityQueryString(projectId, linkingTaskId, activeLearningActivities.activeLearning);
        const cleanUp = connectWebSocket(
            legacyApiEndpoint(`/activities/updatesWebSocket${query}`),
            legacyApiEndpoint(`/activities/updates${query}`),
            onActiveLearningUpdate
        );
        return cleanUp;
    }, []);

    const onActiveLearningUpdate = (activityStatus: IActivityStatus) => {
        if (activityStatus.statusName === "Finished") {
            fetchReferenceLinks(projectId, linkingTaskId);
        }
    };

    /** Fetches the current reference links of the linking task. */
    const fetchReferenceLinks = async (projectId: string, taskId: string) => {
        try {
            setReferenceLinksLoading(true);
            const links = (await fetchActiveLearningReferenceLinks(projectId, taskId)).data;
            setReferenceLinks(links);
        } catch (err) {
            registerError(
                "LinkingRuleEditor_fetchLinkingTask",
                t("taskViews.linkRulesEditor.errors.fetchTaskData.msg"),
                err
            );
        } finally {
            setReferenceLinksLoading(false);
        }
    };

    const loadUnlabeledLinkCandidate = async () => {
        setLoadingLinkCandidate(true);
        try {
            const candidate = (await nextActiveLearningLinkCandidate(projectId, linkingTaskId)).data;
            setSelectedEntityLink(referenceLinksUtils.toReferenceEntityLink(candidate));
        } catch (ex) {
            // TODO
        } finally {
            setLoadingLinkCandidate(false);
        }
    };

    const updateReferenceLink = async (link: EntityLink, decision: ActiveLearningDecisions) => {
        try {
            await submitActiveLearningReferenceLink(
                activeLearningContext.projectId,
                activeLearningContext.linkingTaskId,
                link.source.uri,
                link.target.uri,
                decision
            );
            setSelectedEntityLink(undefined);
        } catch (ex) {
            // TODO
        }
    };

    const onSaveClick = () => {
        // TODO: Open save dialog
    };

    // TODO: i18n
    const Title = () => {
        return (
            <Toolbar>
                <ToolbarSection>Compare resources</ToolbarSection>
                <ToolbarSection canGrow>
                    <Spacing vertical />
                </ToolbarSection>
                <ToolbarSection>
                    <Button
                        title={"Save link rule and/or newly added reference links."}
                        affirmative={true}
                        // Disable if no link rule has been learned and no reference link was added
                        disabled={false}
                        onClick={onSaveClick}
                    >
                        Save
                    </Button>
                </ToolbarSection>
            </Toolbar>
        );
    };

    return (
        <LinkingRuleActiveLearningFeedbackContext.Provider
            value={{
                updateReferenceLink,
                selectedLink: selectedEntityLink,
                loadingLinkCandidate,
            }}
        >
            <div>
                <Title />
                <Spacing hasDivider={true} />
                <LinkingRuleActiveLearningFeedbackComponent />
                <Spacing />
                <LinkingRuleActiveLearningBestLearnedRule rule={{ task: "TODO" }} />
                <Spacing />
                <LinkingRuleReferenceLinks
                    loading={referenceLinksLoading}
                    referenceLinks={referenceLinks}
                    labelPaths={activeLearningContext.labelPaths}
                    removeLink={(link) => updateReferenceLink(link, "unlabeled")}
                />
            </div>
        </LinkingRuleActiveLearningFeedbackContext.Provider>
    );
};
