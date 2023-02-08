import { TextAreaProps } from "@eccenca/gui-elements/src/components/TextField/TextArea";
import { TextArea, Toolbar, ToolbarSection } from "@eccenca/gui-elements";
import React from "react";
import { useInvisibleCharacterCleanUpModal } from "../modals/InvisibleCharacterCleanUpModal";

interface Props extends Omit<TextAreaProps, "value" | "onChange"> {
    onChange: (value: string) => any;
}

/** Uncontrolled TextArea that detects invisible characters and allows to remove them */
export const TextAreaWithCharacterWarnings = ({ onChange, ...otherProps }: Props) => {
    const [inputString, setInputString] = React.useState<string>("");
    const ref = React.useRef<HTMLTextAreaElement>(null);
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
        (e: React.ChangeEvent<HTMLTextAreaElement>) => {
            changeInputString(e.target.value);
        },
        [changeInputString]
    );

    const setCleanInputString = React.useCallback(
        (value: string) => {
            if (ref.current) {
                ref.current.value = value;
                // This needs to be called since the TextArea is uncontrolled and won't call onChange to reset the invisible char hook
                cleanUpRef.current?.();
            }
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
                <TextArea
                    {...otherProps}
                    onChange={onChangeInputElement}
                    invisibleCharacterWarning={invisibleCharacterWarning}
                    inputRef={ref}
                />
            </ToolbarSection>
        </Toolbar>
    );
};
