import React from "react";
import {
    Button,
    IconButton,
    Section,
    SectionHeader,
    TitleMainsection,
    Spacing,
    Toolbar,
    ToolbarSection,
    IActivityStatus,
} from "@eccenca/gui-elements";
import { LinkingRuleActiveLearningContext } from "../contexts/LinkingRuleActiveLearningContext";
import { LinkingRuleActiveLearningFeedbackComponent } from "./LinkingRuleActiveLearningFeedbackComponent";
import { EntityLink, ReferenceLinksOrdered } from "../../referenceLinks/LinkingRuleReferenceLinks.typing";
import { LinkingRuleActiveLearningFeedbackContext } from "../contexts/LinkingRuleActiveLearningFeedbackContext";
import { LinkingRuleActiveLearningBestLearnedRule } from "./LinkingRuleActiveLearningBestLearnedRule";
import { LinkingRuleReferenceLinks } from "../../referenceLinks/LinkingRuleReferenceLinks";
import {
    bestLearnedLinkageRule,
    fetchActiveLearningReferenceLinks,
    nextActiveLearningLinkCandidate,
    submitActiveLearningReferenceLink,
} from "../LinkingRuleActiveLearning.requests";
import referenceLinksUtils from "../../referenceLinks/LinkingRuleReferenceLinks.utils";
import { ActiveLearningDecisions } from "../LinkingRuleActiveLearning.typings";
import { ILinkingRule, OptionallyLabelledParameter } from "../../linking.types";
import useErrorHandler from "../../../../../hooks/useErrorHandler";
import { useTranslation } from "react-i18next";
import { activityQueryString } from "../../../../shared/TaskActivityOverview/taskActivityUtils";
import { connectWebSocket } from "../../../../../services/websocketUtils";
import { legacyApiEndpoint } from "../../../../../utils/getApiEndpoint";
import { activeLearningActivities } from "../LinkingRuleActiveLearning";
import { LinkingRuleActiveLearningSaveModal } from "./LinkingRuleActiveLearningSaveModal";

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
    const [referenceLinks, setReferenceLinks] = React.useState<ReferenceLinksOrdered | undefined>(undefined);
    const [referenceLinksLoading, setReferenceLinksLoading] = React.useState(false);
    const [bestRule, setBestRule] = React.useState<OptionallyLabelledParameter<ILinkingRule> | undefined>(undefined);
    const [showSaveDialog, setShowSaveDialog] = React.useState(false);
    const linkTypeToShow = React.useRef<"labeled" | "unlabeled">("labeled");
    const { registerError } = useErrorHandler();

    React.useEffect(() => {
        if (!selectedEntityLink) {
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

    const onActiveLearningUpdate = (activityStatus: IActivityStatus) => {
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
            // TODO
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
        setShowSaveDialog(true);
    };

    // TODO: i18n
    const Title = () => {
        return (
            <SectionHeader>
                <Toolbar>
                    <ToolbarSection>
                        <TitleMainsection>
                            Compare resources
                        </TitleMainsection>
                    </ToolbarSection>
                    <ToolbarSection canGrow>
                        <Spacing vertical />
                    </ToolbarSection>
                    <ToolbarSection>
                        <IconButton
                            name={"item-remove"}
                            disruptive={true}
                            text={t("ActiveLearning.config.buttons.resetTooltip")}
                            onClick={activeLearningContext.showResetDialog}
                        />
                        <Spacing vertical={true} size="small"/>
                        <Button
                            title={"Save link rule and/or newly added reference links."}
                            affirmative={true}
                            // TODO: Disable if no reference link was added
                            disabled={!bestRule}
                            onClick={onSaveClick}
                        >
                            Save
                        </Button>
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
                <LinkingRuleActiveLearningFeedbackComponent />
                <Spacing />
                <LinkingRuleActiveLearningBestLearnedRule rule={bestRule} score={referenceLinks?.evaluationScore} />
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
                {
                    // TODO: Check that reference links are saved
                    showSaveDialog ? (
                        <LinkingRuleActiveLearningSaveModal
                            unsavedBestRule={!!bestRule}
                            unsavedReferenceLinks={1 /** TODO: Put real number*/}
                            onClose={() => setShowSaveDialog(false)}
                        />
                    ) : null
                }
            </Section>
        </LinkingRuleActiveLearningFeedbackContext.Provider>
    );
};
