import { IRuleOperator } from "../../RuleEditor.typings";
import { RuleEditorNodeParameterValue } from "../../model/RuleEditorModel.typings";

/** The operator data that is used for rendering a rule operator in the sidebar. */
export interface SidebarRuleOperatorBase extends Omit<IRuleOperator, "portSpecification" | "parameterSpecification"> {}

/** A rule operator plugin version that has initial parameter values that are different from the plugin specification. */
export interface IPreConfiguredRuleOperator extends SidebarRuleOperatorBase {
    /** New initial values for this pre-configured operator. */
    parameterOverwrites: {
        [key: string]: RuleEditorNodeParameterValue;
    };
}
