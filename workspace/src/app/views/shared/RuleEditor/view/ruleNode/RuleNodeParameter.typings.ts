import { IParameterSpecification } from "../../RuleEditor.typings";
import { RuleEditorNodeParameterValue } from "../../model/RuleEditorModel.typings";

export interface IRuleNodeParameter {
    /** The ID of the parameter. */
    parameterId: string;
    /** The update function for the value. */
    update: (value: RuleEditorNodeParameterValue) => any;
    /** Fetches the current value for this parameter. */
    currentValue: () => RuleEditorNodeParameterValue;
    /** The initial value of the parameter. */
    initialValue?: RuleEditorNodeParameterValue;
    /** The parameter specification. */
    parameterSpecification: IParameterSpecification;
}
