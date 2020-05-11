import React from "react";
import IconButton from "../Icon/IconButton";
import TextField from "./TextField";

function SearchField({
    className = "",
    emptySearchInputMessage = "Enter search term",
    onClearanceHandler,
    onClearanceText,
    ...otherProps
}: any) {
    return (
        <TextField
            className={
                "ecc-textfield--searchinput" +
                (onClearanceHandler ? " ecc-textfield--justifyclearance" : "") +
                (className ? " " + className : "")
            }
            dir={"auto"}
            placeholder={emptySearchInputMessage}
            aria-label={emptySearchInputMessage}
            rightElement={
                onClearanceHandler && otherProps.value ? (
                    <IconButton
                        name="operation-clear"
                        text={onClearanceText ? onClearanceText : "Clear current search term"}
                        onClick={onClearanceHandler}
                    />
                ) : (
                    false
                )
            }
            {...otherProps}
            type={"search"}
            leftIcon={"operation-search"}
            round={true}
        />
    );
}

export default SearchField;
