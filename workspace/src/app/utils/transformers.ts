import { IArtefactItemProperty } from "@ducks/common/typings";
import { INPUT_TYPES } from "../constants";

/** Converts the default value to a JS value */
export const defaultValueAsJs = (property: IArtefactItemProperty): any => {
    return stringValueAsJs(property.parameterType, property.value);
};
/** Converts a string value to its typed equivalent based on the given value type. */
export const stringValueAsJs = (valueType: string, value: string | null): any => {
    let v: any = value || "";

    if (valueType === INPUT_TYPES.BOOLEAN) {
        // cast to boolean from string
        v = value === "true";
    }

    if (valueType === INPUT_TYPES.INTEGER) {
        if (v !== "" && value) {
            v = parseInt(value);
        } else {
            v = null;
        }
    }
    return v;
};
/** Extracts the initial values from the parameter values of an existing task and turns them into a flat object, e.g. obj["nestedParam.param1"].
 *  If the original values are reified values with optional labels, this reified structure is kept in the flat object.
 **/
export const existingTaskValuesToFlatParameters = (updateTask: any) => {
    if (updateTask) {
        const result: any = {};
        const objToFlatRec = (obj: object, prefix: string) => {
            Object.entries(obj).forEach(([paramName, paramLabelAndValue]) => {
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
                    result[prefix + paramName] = paramLabelAndValue;
                }
            });
        };
        objToFlatRec(updateTask.parameterValues, "");
        return result;
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
