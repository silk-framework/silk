import { StickyNote } from "@eccenca/gui-elements";
import { IValueInput, RuleLayout } from "../shared/rules/rule.typings";

export interface IRuleBlockPort {
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

export interface IRuleBlockUiAnnotations {
    stickyNotes?: StickyNote[];
}

export interface IRuleBlockModel {
    ports: IRuleBlockPort[];
    inputExamples: IRuleBlockInputExample[];
    operatorTree?: IValueInput;
    layout: RuleLayout;
    uiAnnotations?: IRuleBlockUiAnnotations;
}

export interface IRuleBlockTaskParameters {
    ruleBlockModel?: IRuleBlockModel;
}

export interface IRuleBlockInspectionSnapshot {
    ports: IRuleBlockPort[];
    operatorTree?: IValueInput;
    layout: RuleLayout;
    uiAnnotations?: IRuleBlockUiAnnotations;
}

export interface IRuleBlockInspection {
    snapshots: Record<string, IRuleBlockInspectionSnapshot>;
}

export interface IRuleBlockSummary {
    id: string;
    label: string;
    description?: string;
    ports: IRuleBlockPort[];
}
