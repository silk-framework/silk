import { StickyNote } from "@eccenca/gui-elements";
import { IValueInput, RuleLayout } from "../shared/rules/rule.typings";

export interface IRuleBlockPort {
    id: string;
    label: string;
    description: string;
    exampleValues: string;
    displayOrder: number;
    deprecated: boolean;
}

export interface IRuleBlockUiAnnotations {
    stickyNotes?: StickyNote[];
}

export interface IRuleBlockModel {
    ports: IRuleBlockPort[];
    operatorTree?: IValueInput;
    layout: RuleLayout;
    uiAnnotations?: IRuleBlockUiAnnotations;
}

export interface IRuleBlockTaskParameters {
    ruleBlockModel?: IRuleBlockModel;
}
