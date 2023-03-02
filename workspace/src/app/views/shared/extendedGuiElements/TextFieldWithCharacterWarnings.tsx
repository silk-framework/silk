import { TextFieldProps } from "@eccenca/gui-elements/src/components/TextField/TextField";
import { TextField, Toolbar, ToolbarSection } from "@eccenca/gui-elements";
import React from "react";
import { useInvisibleCharacterCleanUpModal } from "../modals/InvisibleCharacterCleanUpModal";

interface Props extends Omit<TextFieldProps, "value" | "onChange"> {
    onChange: (value: string) => any;
}

/** Uncontrolled TextField that detects invisible characters and allows to remove them */
export const TextFieldWithCharacterWarnings = ({ onChange, ...otherProps }: Props) => {
    const [inputString, setInputString] = React.useState<string>("");
    const [updatedDefaultValue, setUpdatedDefaultValue] = React.useState(otherProps.defaultValue);
    const ref = React.useRef<HTMLInputElement>(null);
    // Contains optional clean up function that is called after a string is cleaned
    const cleanUpRef = React.useRef<() => any>(() => {});

    const changeInputString = React.useCallback(
        (newValue: string) => {
            onChange(newValue);
            setInputString(newValue);
        },
        [onChange]
    );

    const onChangeInputElement = React.useCallback(
        (e: React.ChangeEvent<HTMLInputElement>) => {
            changeInputString(e.target.value);
        },
        [changeInputString]
    );

    const setCleanInputString = React.useCallback(
        (value: string) => {
            if (ref.current) {
                ref.current.value = value;
                // This needs to be called since the TextField is uncontrolled and won't call onChange to reset the invisible char hook
                cleanUpRef.current?.();
            }
            setUpdatedDefaultValue(value);
            changeInputString(value);
        },
        [changeInputString]
    );

    const { iconButton, modalElement, invisibleCharacterWarning, resetCleanUpModalComponent } =
        useInvisibleCharacterCleanUpModal({
            inputString: inputString,
            setString: setCleanInputString,
            callbackDelay: 200,
        });
    cleanUpRef.current = resetCleanUpModalComponent;

    return (
        <Toolbar>
            <ToolbarSection canGrow>
                {modalElement}
                {iconButton}
                <TextField
                    {...otherProps}
                    onChange={onChangeInputElement}
                    invisibleCharacterWarning={invisibleCharacterWarning}
                    defaultValue={updatedDefaultValue}
                    inputRef={ref}
                />
            </ToolbarSection>
        </Toolbar>
    );
};
