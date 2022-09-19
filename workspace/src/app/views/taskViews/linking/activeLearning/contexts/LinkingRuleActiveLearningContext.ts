import React from "react";
import { TaskPlugin } from "@ducks/shared/typings";
import { ILinkingRule, ILinkingTaskParameters } from "../../linking.types";
import { ActiveLearningStep, ComparisonPairWithId } from "../LinkingRuleActiveLearning.typings";
import { LabelProperties } from "../../referenceLinks/LinkingRuleReferenceLinks.typing";
import { IRuleOperatorNode } from "../../../../shared/RuleEditor/RuleEditor.typings";

interface LinkingRuleActiveLearningContextProps {
    projectId: string;
    linkingTaskId: string;
    /** The current linking task. */
    linkTask?: TaskPlugin<ILinkingTaskParameters>;
    /** The properties that should be compared between the 2 data sources. */
    propertiesToCompare: ComparisonPairWithId[];
    /** Update which properties should be compared. */
    setPropertiesToCompare: React.Dispatch<React.SetStateAction<ComparisonPairWithId[]>>;
    /** Navigate to a different view. */
    navigateTo: (step: ActiveLearningStep | "linkingEditor") => void;
    /** The source paths of the label values that should be displayed in the UI for each entity in a link. */
    labelPaths?: LabelProperties;
    /** Change the label paths. */
    changeLabelPaths: (paths: LabelProperties) => any;
    /** True while comparison pair activity is running. */
    comparisonPairsLoading: boolean;
    /** Show dialog to reset the state of the current learning session. */
    showResetDialog: () => any;
    /** Converts a linking rule to rule operators. */
    convertRule: (linkingRule: ILinkingRule) => IRuleOperatorNode[];
    /** Basically if the user has already clicked on the start learning button. */
    learningStarted: boolean;
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
    showResetDialog: () => {},
    convertRule: () => [],
    learningStarted: false,
});
