import React from "react";
import { Button, Spacing, Toolbar, ToolbarSection } from "@eccenca/gui-elements";
import { LinkingRuleActiveLearningContext } from "../contexts/LinkingRuleActiveLearningContext";
import { LinkingRuleActiveLearningFeedbackComponent } from "./LinkingRuleActiveLearningFeedbackComponent";
import { EntityLink } from "../../referenceLinks/LinkingRuleReferenceLinks.typing";
import { LinkingRuleActiveLearningFeedbackContext } from "../contexts/LinkingRuleActiveLearningFeedbackContext";
import { LinkingRuleActiveLearningBestLearnedRule } from "./LinkingRuleActiveLearningBestLearnedRule";
import { LinkingRuleReferenceLinks } from "../../referenceLinks/LinkingRuleReferenceLinks";
import {
    nextActiveLearningLinkCandidate,
    submitActiveLearningReferenceLink,
} from "../LinkingRuleActiveLearning.requests";
import referenceLinksUtils from "../../referenceLinks/LinkingRuleReferenceLinks.utils";
import { ActiveLearningDecisions } from "../LinkingRuleActiveLearning.typings";

interface LinkingRuleActiveLearningMainProps {
    projectId: string;
    linkingTaskId: string;
}

/**
 * The main step of the active learning process that generates a gold standard through active learning
 * and learns a linking rule.
 */
export const LinkingRuleActiveLearningMain = ({ projectId, linkingTaskId }: LinkingRuleActiveLearningMainProps) => {
    const activeLearningContext = React.useContext(LinkingRuleActiveLearningContext);
    /** The list of unlabeled entity link candidates suggested from the active learning algorithm ordered by relevance to the algorithm. */
    const [unlabeledLinkCandidate, setUnlabeledLinkCandidate] = React.useState<EntityLink | undefined>(undefined);
    const [loadingLinkCandidate, setloadingLinkCandidate] = React.useState(false);
    /** The currently selected entity, either an unlabeled link or an existing reference link. */
    const [selectedEntityLink, setSelectedEntityLink] = React.useState<EntityLink | undefined>(undefined);

    React.useEffect(() => {
        if (!selectedEntityLink && unlabeledLinkCandidate != null) {
            setSelectedEntityLink(unlabeledLinkCandidate);
            setUnlabeledLinkCandidate(undefined);
        }
    }, [selectedEntityLink, unlabeledLinkCandidate]);

    React.useEffect(() => {
        if (!unlabeledLinkCandidate) {
            loadUnlabeledLinkCandidate();
        }
    }, [unlabeledLinkCandidate]);

    const loadUnlabeledLinkCandidate = async () => {
        setloadingLinkCandidate(true);
        try {
            const candidate = (await nextActiveLearningLinkCandidate(projectId, linkingTaskId)).data;
            setUnlabeledLinkCandidate(referenceLinksUtils.toReferenceEntityLink(candidate));
        } catch (ex) {
            // TODO
        } finally {
            setloadingLinkCandidate(false);
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
                    referenceLinks={activeLearningContext.referenceLinks}
                    labelPaths={activeLearningContext.labelPaths}
                />
            </div>
        </LinkingRuleActiveLearningFeedbackContext.Provider>
    );
};
