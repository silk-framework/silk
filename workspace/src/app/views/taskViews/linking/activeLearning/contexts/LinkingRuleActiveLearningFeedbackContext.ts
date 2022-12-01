import React from "react";

import { EntityLink } from "../../referenceLinks/LinkingRuleReferenceLinks.typing";
import {
    ActiveLearningDecisions,
    ActiveLearningLinkCandidate,
    ActiveLearningReferenceLink,
} from "../LinkingRuleActiveLearning.typings";

interface LinkingRuleActiveLearningFeedbackContextProps {
    /** Add, update or remove (un-label) a reference link. */
    updateReferenceLink(
        entityLink: EntityLink | ActiveLearningLinkCandidate,
        decision: ActiveLearningDecisions
    ): Promise<void> | void;
    /** Configured */
    /** The currently selected link. */
    selectedLink: ActiveLearningReferenceLink | ActiveLearningLinkCandidate | undefined;
    /** Cancel selected link. This only has an effect if a link was selected e.g. from the reference links. */
    cancel: () => any;
    /** True while loading a link candidate. */
    loadingLinkCandidate: boolean;
}

/** Contains data and functions for the link rule active learning. */
export const LinkingRuleActiveLearningFeedbackContext =
    React.createContext<LinkingRuleActiveLearningFeedbackContextProps>({
        updateReferenceLink() {},
        selectedLink: undefined,
        loadingLinkCandidate: false,
        cancel: () => {},
    });
