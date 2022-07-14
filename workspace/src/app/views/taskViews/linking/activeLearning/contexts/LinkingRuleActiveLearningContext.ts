import React from "react";
import { TaskPlugin } from "@ducks/shared/typings";
import { ILinkingTaskParameters } from "../../linking.types";
import { CandidatePropertyPair } from "../LinkingRuleActiveLearning.typings";

interface LinkingRuleActiveLearningContextProps {
    projectId: string;
    linkingTaskId: string;
    linkTask?: TaskPlugin<ILinkingTaskParameters>;
    propertiesToCompare: CandidatePropertyPair[];
    setPropertiesToCompare: (pairs: CandidatePropertyPair[]) => any;
}

/** Contains data and functions for the link rule active learning. */
export const LinkingRuleActiveLearningContext = React.createContext<LinkingRuleActiveLearningContextProps>({
    projectId: "",
    linkingTaskId: "",
    propertiesToCompare: [],
    setPropertiesToCompare: () => {},
});
