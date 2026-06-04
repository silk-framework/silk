import { StickyNote } from "@eccenca/gui-elements";
import { IValueInput, RuleLayout } from "../shared/rules/rule.typings";

export interface RuleBlockPort {
    id: string;
    label: string;
    description: string;
    displayOrder: number;
    deprecated: boolean;
}

export interface IRuleBlockInputExample {
    id: string;
    label?: string;
    inputs: Record<string, string[]>;
}

export interface RuleBlockUiAnnotations {
    stickyNotes?: StickyNote[];
}

export interface IRuleBlockModel {
    ports: RuleBlockPort[];
    inputExamples: IRuleBlockInputExample[];
    operatorTree?: IValueInput;
    layout: RuleLayout;
    uiAnnotations?: RuleBlockUiAnnotations;
}

export interface IRuleBlockTaskParameters {
    ruleBlockModel?: IRuleBlockModel;
}

/** The relevant data of a rule block that allows evaluating it with external values. */
export interface RuleBlockSnapshot {
    ports: RuleBlockPort[];
    operatorTree?: IValueInput;
    layout: RuleLayout;
    uiAnnotations?: RuleBlockUiAnnotations;
}

export interface IRuleBlockSnapshots {
    snapshots: Record<string, RuleBlockSnapshot>;
}

export interface IRuleBlockSummary {
    id: string;
    label: string;
    description?: string;
    ports: RuleBlockPort[];
}
