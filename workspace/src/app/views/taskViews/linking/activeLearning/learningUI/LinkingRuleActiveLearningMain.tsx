import React from "react";
import { Button, Spacing, Toolbar, ToolbarSection } from "@eccenca/gui-elements";
import useErrorHandler from "../../../../../hooks/useErrorHandler";
import { LinkingRuleActiveLearningContext } from "../contexts/LinkingRuleActiveLearningContext";
import { useTranslation } from "react-i18next";
import { LinkingRuleActiveLearningFeedbackComponent } from "./LinkingRuleActiveLearningFeedbackComponent";
import { CandidatePropertyPair } from "../LinkingRuleActiveLearning.typings";
import { EntityLink } from "./LinkingRuleActiveLearningMain.typings";
import { LinkingRuleActiveLearningFeedbackContext } from "../contexts/LinkingRuleActiveLearningFeedbackContext";

interface LinkingRuleActiveLearningMainProps {
    projectId: string;
    linkingTaskId: string;
}

/** TODO: Fetch entity link suggestions from backend and remove this mock. */
const mockEntityLinks: EntityLink[] = [
    {
        entityLinkId: "suggested-1",
        label: "unlabeled",
        source: {
            uri: "source-1",
            values: [["Source label"], ["x"], ["123"]],
        },
        target: {
            uri: "target-1",
            values: [["Target entity label"], ["y"], ["ZZZ"]],
        },
    },
];

/**
 * The main step of the active learning process that generates a gold standard through active learning
 * and learns a linking rule.
 */
export const LinkingRuleActiveLearningMain = ({ projectId, linkingTaskId }: LinkingRuleActiveLearningMainProps) => {
    const [t] = useTranslation();
    const { registerError } = useErrorHandler();
    const activeLearningContext = React.useContext(LinkingRuleActiveLearningContext);
    /** The list of unlabeled entity link candidates suggested from the active learning algorithm ordered by relevance to the algorithm. */
    const [unlabeledLinkCandidates, setUnlabeledLinkCandidates] = React.useState<EntityLink[]>(mockEntityLinks);
    /** The currently selected entity, either an unlabeled link or an existing reference link. */
    const [selectedEntityLink, setSelectedEntityLink] = React.useState<EntityLink | undefined>(undefined);

    React.useEffect(() => {
        if (!selectedEntityLink && unlabeledLinkCandidates.length > 0) {
            setSelectedEntityLink(unlabeledLinkCandidates[0]);
            setUnlabeledLinkCandidates([...unlabeledLinkCandidates.slice(1)]);
        }
    }, [selectedEntityLink, unlabeledLinkCandidates]);

    const removeReferenceLink = (linkId: string) => {};

    const updateReferenceLink = (entityLink: EntityLink, decision: "positive" | "negative") => {};

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
                removeReferenceLink: removeReferenceLink,
                updateReferenceLink: updateReferenceLink,
                selectedLink: selectedEntityLink,
            }}
        >
            <div>
                <Title />
                <Spacing hasDivider={true} />
                <LinkingRuleActiveLearningFeedbackComponent />
            </div>
        </LinkingRuleActiveLearningFeedbackContext.Provider>
    );
};
