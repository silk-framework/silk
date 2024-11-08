import React from "react";
import {
    Button,
    SilkActivityStatusProps,
    IconButton,
    Section,
    SectionHeader,
    Spacing,
    TitleMainsection,
    Toolbar,
    ToolbarSection,
} from "@eccenca/gui-elements";
import { LinkingRuleActiveLearningContext } from "../contexts/LinkingRuleActiveLearningContext";
import { LinkingRuleActiveLearningFeedbackComponent } from "./LinkingRuleActiveLearningFeedbackComponent";
import { LinkingRuleActiveLearningFeedbackContext } from "../contexts/LinkingRuleActiveLearningFeedbackContext";
import { LinkingRuleActiveLearningBestLearnedRule } from "./LinkingRuleActiveLearningBestLearnedRule";
import { LinkingRuleReferenceLinks } from "../../referenceLinks/LinkingRuleReferenceLinks";
import {
    bestLearnedLinkageRule,
    fetchActiveLearningReferenceLinks,
    nextActiveLearningLinkCandidate,
    submitActiveLearningReferenceLink,
} from "../LinkingRuleActiveLearning.requests";
import {
    ActiveLearningBestRule,
    ActiveLearningDecisions,
    ActiveLearningLinkCandidate,
    ActiveLearningReferenceLink,
    ActiveLearningReferenceLinks,
} from "../LinkingRuleActiveLearning.typings";
import useErrorHandler from "../../../../../hooks/useErrorHandler";
import { useTranslation } from "react-i18next";
import { activityQueryString } from "../../../../shared/TaskActivityOverview/taskActivityUtils";
import { connectWebSocket } from "../../../../../services/websocketUtils";
import { legacyApiEndpoint } from "../../../../../utils/getApiEndpoint";
import { activeLearningActivities } from "../LinkingRuleActiveLearning";
import { LinkingRuleActiveLearningSaveModal } from "./LinkingRuleActiveLearningSaveModal";
import { useActiveLearningSessionInfo } from "../shared/ActiveLearningSessionInfoWidget";
import { FetchError } from "../../../../../services/fetch/responseInterceptor";

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
    /** The currently selected entity link, either an link candidate or an existing reference link. */
    const [selectedEntityLink, setSelectedEntityLink] = React.useState<
        ActiveLearningLinkCandidate | ActiveLearningReferenceLink | undefined
    >(undefined);
    /** The list of reference links. */
    const [referenceLinks, setReferenceLinks] = React.useState<ActiveLearningReferenceLinks | undefined>(undefined);
    const [referenceLinksInitiallyLoaded, setReferenceLinksInitiallyLoaded] = React.useState(false);
    const [referenceLinksLoading, setReferenceLinksLoading] = React.useState(false);
    const [bestRule, setBestRule] = React.useState<ActiveLearningBestRule | undefined>(undefined);
    const [unsavedStateExists, setUnsavedStateExists] = React.useState(false);
    const [showSaveDialog, setShowSaveDialog] = React.useState(false);
    const linkTypeToShow = React.useRef<"labeled" | "unlabeled">("labeled");
    const { sessionInfo } = useActiveLearningSessionInfo(projectId, linkingTaskId);
    const { registerError } = useErrorHandler();

    React.useEffect(() => {
        if (!selectedEntityLink || !referenceLinksInitiallyLoaded) {
            setReferenceLinksInitiallyLoaded(true);
            fetchReferenceLinks();
            loadUnlabeledLinkCandidate();
        }
    }, [selectedEntityLink]);

    React.useEffect(() => {
        const query = activityQueryString(projectId, linkingTaskId, activeLearningActivities.activeLearning);
        const cleanUp = connectWebSocket(
            legacyApiEndpoint(`/activities/updatesWebSocket${query}`),
            legacyApiEndpoint(`/activities/updates${query}`),
            onActiveLearningUpdate
        );
        return cleanUp;
    }, []);

    React.useEffect(() => {
        if (sessionInfo) {
            const changes = sessionInfo.referenceLinks;
            if (changes.addedLinks || changes.removedLinks) {
                setUnsavedStateExists(true);
            }
        }
    }, [sessionInfo]);

    const onActiveLearningUpdate = (activityStatus: SilkActivityStatusProps) => {
        if (activityStatus.statusName === "Finished") {
            fetchReferenceLinks();
            updateBestLearnedRule();
        }
    };

    const updateBestLearnedRule = async () => {
        try {
            const rule = (await bestLearnedLinkageRule(projectId, linkingTaskId)).data;
            setBestRule(rule);
        } catch (err) {
            if (err.isHttpError && (err as FetchError).httpStatus !== 404) {
                registerError(
                    "activeLearning-updateBestLearnedRule",
                    "Currently best learned rule could not be fetched from the backend.",
                    err
                );
            }
        }
    };

    /** Fetches the current reference links of the linking task. */
    const fetchReferenceLinks = async () => {
        const unlabeled = linkTypeToShow.current === "unlabeled";
        try {
            setReferenceLinksLoading(true);
            const links = (
                await fetchActiveLearningReferenceLinks(projectId, linkingTaskId, !unlabeled, !unlabeled, unlabeled)
            ).data;
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
            setSelectedEntityLink(candidate);
        } catch (ex) {
            registerError(
                "LearningMain.loadUnlabeledLinkCandidate",
                t("ActiveLearning.feedback.couldNotLoadCandidateError"),
                ex
            );
        } finally {
            setLoadingLinkCandidate(false);
        }
    };

    const updateReferenceLink = async (link: ActiveLearningLinkCandidate, decision: ActiveLearningDecisions) => {
        let { source, target } = link;
        try {
            await submitActiveLearningReferenceLink(
                activeLearningContext.projectId,
                activeLearningContext.linkingTaskId,
                source,
                target,
                decision
            );
            setSelectedEntityLink(undefined);
        } catch (ex) {
            registerError("activeLearning-updateReferenceLink", "Updating reference links has failed.", ex);
        }
    };

    const onSaveClick = () => {
        setShowSaveDialog(true);
    };

    const Title = () => {
        return (
            <SectionHeader>
                <Toolbar>
                    <ToolbarSection>
                        <TitleMainsection>{t("ActiveLearning.toolbar.title")}</TitleMainsection>
                    </ToolbarSection>
                    <ToolbarSection canGrow>
                        <Spacing vertical />
                    </ToolbarSection>
                    <ToolbarSection>
                        <IconButton
                            name={"item-reset"}
                            disruptive={true}
                            text={t("ActiveLearning.config.buttons.resetTooltip")}
                            onClick={activeLearningContext.showResetDialog}
                        />
                        <Spacing vertical={true} size="small" />
                        <IconButton
                            text={t("ActiveLearning.feedback.propertyConfiguration")}
                            name={"item-settings"}
                            onClick={() => activeLearningContext.navigateTo("config")}
                        />
                        <Spacing vertical={true} size="small" />
                        <Button
                            data-test-id={"save-active-learning-state-btn"}
                            text={t("common.action.save")}
                            title={t("ActiveLearning.saveDialog.title")}
                            affirmative={true}
                            disabled={!bestRule && !unsavedStateExists}
                            onClick={onSaveClick}
                        />
                    </ToolbarSection>
                </Toolbar>
            </SectionHeader>
        );
    };

    const cancelSelection = () => {
        setSelectedEntityLink(undefined);
    };

    const showLinkType = (type: "labeled" | "unlabeled") => {
        if (type !== linkTypeToShow.current) {
            linkTypeToShow.current = type;
            fetchReferenceLinks();
        }
    };

    const onClose = (saved: boolean, ruleSaved: boolean, saveReferenceLinks: boolean) => {
        setShowSaveDialog(false);
        if (!saved) return;
        if (saveReferenceLinks) {
            setUnsavedStateExists(false);
        }
        if (ruleSaved) {
            activeLearningContext.navigateTo("linkingEditor");
        } else {
            activeLearningContext.navigateTo("config");
        }
    };

    return (
        <LinkingRuleActiveLearningFeedbackContext.Provider
            value={{
                updateReferenceLink,
                selectedLink: selectedEntityLink,
                loadingLinkCandidate,
                cancel: cancelSelection,
            }}
        >
            <Section>
                <Title />
                <Spacing />
                <LinkingRuleActiveLearningFeedbackComponent setUnsavedStateExists={() => setUnsavedStateExists(true)} />
                <Spacing />
                <LinkingRuleActiveLearningBestLearnedRule rule={bestRule} />
                <Spacing />
                <LinkingRuleReferenceLinks
                    title={t("ActiveLearning.referenceLinks.title")}
                    loading={referenceLinksLoading}
                    referenceLinks={referenceLinks}
                    labelPaths={activeLearningContext.labelPaths}
                    removeLink={(link) => updateReferenceLink(link, "unlabeled")}
                    openLink={(link) => setSelectedEntityLink(link)}
                    showLinkType={showLinkType}
                />
                {showSaveDialog ? (
                    <LinkingRuleActiveLearningSaveModal unsavedBestRule={bestRule} onClose={onClose} />
                ) : null}
            </Section>
        </LinkingRuleActiveLearningFeedbackContext.Provider>
    );
};
