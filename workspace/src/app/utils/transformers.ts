import { IArtefactItemProperty } from "@ducks/common/typings";
import { INPUT_TYPES } from "../constants";
import {
    OptionallyLabelledParameter,
    optionallyLabelledParameterToLabel,
    optionallyLabelledParameterToValue,
} from "../views/taskViews/linking/linking.types";
import { UpdateTaskProps } from "../views/shared/modals/CreateArtefactModal/ArtefactForms/TaskForm";

/** Converts the default value to a JS value */
export const defaultValueAsJs = (property: IArtefactItemProperty, withLabel: boolean): any => {
    const value = stringValueAsJs(property.parameterType, property.value);
    return withLabel ? { value, label: optionallyLabelledParameterToLabel(property.value) } : value;
};
/** Converts a string value to its typed equivalent based on the given value type. */
export const stringValueAsJs = (valueType: string, value: OptionallyLabelledParameter<string> | null): any => {
    const stringValue = value != null ? optionallyLabelledParameterToValue(value) ?? "" : "";
    let v: any = stringValue;

    if (valueType === INPUT_TYPES.BOOLEAN) {
        // cast to boolean from string
        v = stringValue.toLowerCase() === "true";
    }

    if (valueType === INPUT_TYPES.INTEGER) {
        if (v !== "" && stringValue) {
            v = parseInt(stringValue);
        } else {
            v = null;
        }
    }
    return v;
};

/**
 * Turns a nested object to a flat object, e.g. {parent: {child: "value"}} becomes {"parent.child": "value"}.
 * @param object            The input object.
 * @param replacementValues Replacement values for specific parameters.
 * @param labelValueFormat  If true all values that will be returned have the format {label?: string, value: any}
 */
export const objectToFlatRecord = (
    object: object,
    replacementValues: Record<string, any>,
    labelValueFormat: boolean
): Record<string, any> => {
    const result: any = Object.create(null);
    const objToFlatRec = (obj: object, prefix: string) => {
        Object.entries(obj).forEach(([paramName, paramLabelAndValue]) => {
            const fullParameterId = `${prefix}${paramName}`;
            let paramValue = paramLabelAndValue;
            if (
                paramLabelAndValue !== null &&
                paramLabelAndValue.value !== undefined &&
                (Object.entries(paramLabelAndValue).length === 1 ||
                    (Object.entries(paramLabelAndValue).length === 2 && paramLabelAndValue.label !== undefined))
            ) {
                paramValue = paramLabelAndValue.value;
            }
            if (typeof paramValue === "object" && paramValue !== null) {
                objToFlatRec(paramValue, paramName + ".");
            } else {
                result[prefix + paramName] =
                    replacementValues[fullParameterId] != null
                        ? { value: replacementValues[fullParameterId] }
                        : paramLabelAndValue?.value != null || !labelValueFormat
                        ? paramLabelAndValue
                        : { value: paramLabelAndValue };
            }
        });
    };
    objToFlatRec(object, "");
    return result;
};
/** Extracts the initial values from the parameter values of an existing task and turns them into a flat object, e.g. obj["nestedParam.param1"].
 *  If the original values are reified values with optional labels, this reified structure is kept in the flat object.
 **/
export const existingTaskValuesToFlatParameters = (updateTask: Pick<UpdateTaskProps, "parameterValues" | "variableTemplateValues"> | undefined) => {
    if (updateTask) {
        return objectToFlatRecord(updateTask.parameterValues, updateTask.variableTemplateValues, true);
    } else {
        return {};
    }
};

export const uppercaseFirstChar = (str: string) => {
    if (!str || str.length === 0) {
        return str;
    } else {
        return str.charAt(0).toUpperCase() + str.substring(1);
    }
};
