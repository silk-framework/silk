import { EntityLink } from "../learningUI/LinkingRuleActiveLearningMain.typings";
import React from "react";

interface LinkingRuleActiveLearningFeedbackContextProps {
    /** Add or update a reference link. */
    updateReferenceLink(entityLink: EntityLink, decision: "positive" | "negative"): void;
    /** Remove a link from the reference link, i.e. it becomes unlabeled again. */
    removeReferenceLink(entityLinkId: string): void;
    /** Configured */
    /** The currently selected link. */
    selectedLink: EntityLink | undefined;
}

/** Contains data and functions for the link rule active learning. */
export const LinkingRuleActiveLearningFeedbackContext =
    React.createContext<LinkingRuleActiveLearningFeedbackContextProps>({
        removeReferenceLink() {},
        updateReferenceLink() {},
        selectedLink: undefined,
    });