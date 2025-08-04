import { IRuleNodeParameter } from "./RuleNodeParameter.typings";
import React from "react";
import { RuleNodeFormParameter } from "./RuleNodeFormParameter";
import { RuleEditorUiContext } from "../../contexts/RuleEditorUiContext";
import { partitionArray } from "../../../../../utils/basicUtils";
import { AdvancedOptionsArea } from "../../../AdvancedOptionsArea/AdvancedOptionsArea";
import { InputPathFunctions } from "./PathInputOperator";

export interface RuleNodeParametersProps {
    nodeId: string;
    /** Plugin ID of the operator. */
    pluginId: string;
    parameters: IRuleNodeParameter[];
    /** Requests values of parameters this parameter might depend on for auto-completion. */
    dependentValue: (paramId: string) => string | undefined;
    /** The default value as defined in the parameter spec. */
    parameterDefaultValue: (paramId: string) => string | undefined;
    /** If the form will be rendered in a large area. The used input components might differ. */
    large: boolean;
    /** If this is true then the parameters are put into an advanced section that is collapsed by default. */
    hasAdvancedSection?: boolean;
    /** When used inside a modal, the behavior of some components will be optimized. */
    insideModal: boolean;
    /** Functions that are specific to input path rule operators. */
    inputPathFunctions: InputPathFunctions;
}

/** The parameter widget of a rule node. */
export const RuleNodeParameterForm = ({
    nodeId,
    pluginId,
    parameters,
    dependentValue,
    large,
    hasAdvancedSection,
    insideModal,
    inputPathFunctions,
    parameterDefaultValue,
}: RuleNodeParametersProps) => {
    const ruleEditorUiContext = React.useContext(RuleEditorUiContext);
    const { matches: normalParameters, nonMatches: advancedParameters } = partitionArray(
        parameters,
        (param) => !param.parameterSpecification.advanced,
    );

    const shownParameters = [...normalParameters];
    const advancedSectionParameters =
        hasAdvancedSection || ruleEditorUiContext.advancedParameterModeEnabled ? advancedParameters : [];
    const renderFormParameter = (param: IRuleNodeParameter) => {
        return (
            <RuleNodeFormParameter
                key={param.parameterId}
                nodeId={nodeId}
                parameter={param}
                dependentValue={dependentValue}
                parameterDefaultValue={parameterDefaultValue}
                pluginId={pluginId}
                large={large}
                insideModal={insideModal}
                inputPathFunctions={inputPathFunctions}
            />
        );
    };

    return (
        <div key={"ruleNodeParameters"}>
            {shownParameters.map(renderFormParameter)}
            {advancedSectionParameters.length > 0 ? (
                <AdvancedOptionsArea
                    open={ruleEditorUiContext.advancedParameterModeEnabled}
                    compact={!hasAdvancedSection}
                >
                    {advancedSectionParameters.map(renderFormParameter)}
                </AdvancedOptionsArea>
            ) : null}
        </div>
    );
};
