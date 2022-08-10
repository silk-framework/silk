import { EntityLink } from "../../referenceLinks/LinkingRuleReferenceLinks.typing";
import React from "react";
import { ActiveLearningDecisions } from "../LinkingRuleActiveLearning.typings";

interface LinkingRuleActiveLearningFeedbackContextProps {
    /** Add, update or remove (un-label) a reference link. */
    updateReferenceLink(entityLink: EntityLink, decision: ActiveLearningDecisions): Promise<void> | void;
    /** Configured */
    /** The currently selected link. */
    selectedLink: EntityLink | undefined;
    /** True while loading a link candidate. */
    loadingLinkCandidate: boolean;
}

/** Contains data and functions for the link rule active learning. */
export const LinkingRuleActiveLearningFeedbackContext =
    React.createContext<LinkingRuleActiveLearningFeedbackContextProps>({
        updateReferenceLink() {},
        selectedLink: undefined,
        loadingLinkCandidate: false,
    });
