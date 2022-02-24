import { IParameterSpecification, RuleParameterType } from "../../RuleEditor.typings";

export interface IRuleNodeParameter {
    /** The ID of the parameter. */
    parameterId: string;
    /** The type of the parameter, i.e. determines which UI widget to use. */
    parameterType: RuleParameterType;
    /** The update function for the value. */
    update: (value: string) => void;
    /** Fetches the current value for this parameter. */
    currentValue: () => string | undefined;
    /** The initial value of the parameter. */
    initialValue?: string;
    /** The parameter specification. */
    parameterSpecification: IParameterSpecification;
}
