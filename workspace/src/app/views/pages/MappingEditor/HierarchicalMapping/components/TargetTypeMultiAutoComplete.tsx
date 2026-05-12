import React from "react";
import {MultiSuggestField, MultiSuggestFieldSelectionProps, FieldItem, TestableComponent} from "@eccenca/gui-elements";
import silkRestApi from "../../api/silkRestApi";
import { GlobalMappingEditorContext } from "../../contexts/GlobalMappingEditorContext";
import { TargetPropertyAutoCompletion } from "../../api/types";
import { removeExtraSpaces } from "@eccenca/gui-elements/src/common/utils/stringUtils";

interface Props extends TestableComponent {
    onChange: (changes: MultiSuggestFieldSelectionProps<string | TargetPropertyAutoCompletion>) => void;
    placeholder: string;
    className: string;
    isValidNewOption: (text: string) => boolean;
    value: (string | Omit<TargetPropertyAutoCompletion, "extra">)[];
}

/** Multi-selection auto-complete component */
const TargetTypeMultiAutoComplete = ({ placeholder, className, isValidNewOption, value, onChange, ...otherProps }: Props) => {
    const mappingEditorContext = React.useContext(GlobalMappingEditorContext);
    const runOnQueryChange = React.useCallback(
        (query: string) => {
            return silkRestApi.targetClassAutoCompletions(
                mappingEditorContext.projectId,
                mappingEditorContext.transformTaskId,
                query,
                50,
            ).the;
        },
        [mappingEditorContext.projectId, mappingEditorContext.transformTaskId],
    );
    return (
        <FieldItem labelProps={{ text: placeholder }}>
            <MultiSuggestField<Omit<TargetPropertyAutoCompletion, "extra"> | string>
                className={className}
                prePopulateWithItems={!!value?.length}
                openOnKeyDown
                itemId={(t) => (typeof t === "string" ? t : t.value)}
                itemLabel={(t) => (typeof t === "string" ? t : (t.label ?? t.value))}
                items={value}
                onSelection={onChange}
                runOnQueryChange={runOnQueryChange}
                newItemCreationText={"Create option"}
                inputProps={{
                    placeholder: placeholder,
                    ...otherProps

                }}
                tagInputProps={{
                    placeholder: placeholder,
                }}
                isValidNewOption={isValidNewOption}
                createNewItemFromQuery={(query) => ({
                    value: removeExtraSpaces(query),
                })}
                requestDelay={200}
                clearQueryOnSelection={true} // workaround that another Tab does not uncheck matches
            />
        </FieldItem>
    );
};

export default TargetTypeMultiAutoComplete;
