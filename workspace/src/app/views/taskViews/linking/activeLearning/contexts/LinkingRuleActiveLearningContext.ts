import React from "react";
import { TaskPlugin } from "@ducks/shared/typings";
import { ILinkingTaskParameters } from "../../linking.types";
import { ActiveLearningStep, ComparisonPairWithId } from "../LinkingRuleActiveLearning.typings";
import { LabelProperties } from "../../referenceLinks/LinkingRuleReferenceLinks.typing";

interface LinkingRuleActiveLearningContextProps {
    projectId: string;
    linkingTaskId: string;
    /** The current linking task. */
    linkTask?: TaskPlugin<ILinkingTaskParameters>;
    /** The properties that should be compared between the 2 data sources. */
    propertiesToCompare: ComparisonPairWithId[];
    /** Update which properties should be compared. */
    setPropertiesToCompare: (pairs: ComparisonPairWithId[]) => any;
    /** Navigate to a different view. */
    navigateTo: (step: ActiveLearningStep) => void;
    /** The source paths of the label values that should be displayed in the UI for each entity in a link. */
    labelPaths?: LabelProperties;
    /** Change the label paths. */
    changeLabelPaths: (paths: LabelProperties) => any;
    /** True while comparison pair activity is running. */
    comparisonPairsLoading: boolean;
}

/** Contains data and functions for the link rule active learning. */
export const LinkingRuleActiveLearningContext = React.createContext<LinkingRuleActiveLearningContextProps>({
    projectId: "",
    linkingTaskId: "",
    propertiesToCompare: [],
    setPropertiesToCompare: () => {},
    navigateTo: () => {},
    labelPaths: undefined,
    changeLabelPaths: () => {},
    comparisonPairsLoading: false,
});
