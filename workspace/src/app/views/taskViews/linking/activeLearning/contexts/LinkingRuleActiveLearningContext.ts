import React from "react";
import { TaskPlugin } from "@ducks/shared/typings";
import { ILinkingTaskParameters } from "../../linking.types";
import { ActiveLearningStep, CandidatePropertyPair } from "../LinkingRuleActiveLearning.typings";
import { EntityLink } from "../learningUI/LinkingRuleActiveLearningMain.typings";

interface LinkingRuleActiveLearningContextProps {
    projectId: string;
    linkingTaskId: string;
    /** The current linking task. */
    linkTask?: TaskPlugin<ILinkingTaskParameters>;
    /** The properties that should be compared between the 2 data sources. */
    propertiesToCompare: CandidatePropertyPair[];
    /** Update which properties should be compared. */
    setPropertiesToCompare: (pairs: CandidatePropertyPair[]) => any;
    /** Navigate to a different view. */
    navigateTo: (step: ActiveLearningStep) => void;
    /** The current reference links of the linking rule. */
    referenceLinks: EntityLink[];
}

/** Contains data and functions for the link rule active learning. */
export const LinkingRuleActiveLearningContext = React.createContext<LinkingRuleActiveLearningContextProps>({
    projectId: "",
    linkingTaskId: "",
    propertiesToCompare: [],
    setPropertiesToCompare: () => {},
    navigateTo: () => {},
    referenceLinks: [],
});
