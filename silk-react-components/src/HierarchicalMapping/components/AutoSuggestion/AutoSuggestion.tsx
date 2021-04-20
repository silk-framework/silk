import React from "react";
import Dropdown from "react-dropdown";
import { Icon } from "@eccenca/gui-elements";

import { CodeEditor } from "../CodeEditor";

require("./AutoSuggestion.scss");

const AutoSuggestion = ({
    onEditorParamsChange,
    suggestions = [],
    checkPathValidity,
    pathIsValid,
}) => {
    const [value, setValue] = React.useState("");
    const [inputString, setInputString] = React.useState("");
    const [cursorPosition, setCursorPosition] = React.useState(0);
    const [coords, setCoords] = React.useState({});
    const [shouldShowDropdown, setShouldShowDropdown] = React.useState(false);
    const [inputSelection, setInputSelection] = React.useState("");
    const [replacementIndexes, setReplacementIndexes] = React.useState({from:0, length: 0});

    React.useEffect(() => {
        setInputString(() => value);
        setShouldShowDropdown(true);
        checkPathValidity(inputString);
        onEditorParamsChange(
            inputString,
            cursorPosition,
            handleReplacementIndexes
        );
    }, [cursorPosition, value, inputString]);

    const handleChange = (val) => {
        setValue(val);
    };

    const handleCursorChange = (pos, coords) => {
        setCursorPosition(pos.ch);
        setCoords(() => coords);
    };

    const handleInputEditorSelection = (selectedText) => {
        setInputSelection(selectedText);
    };

    const handleReplacementIndexes = (indexes) => {
        setReplacementIndexes(() => ({ ...indexes }));
    };

    const handleDropdownChange = (item) => {
        const { from, length } = replacementIndexes;
        const to = from + length;
        setValue(
            (value) =>
                `${value.substring(0, from)}${item.value}${value.substring(to)}`
        );
        setShouldShowDropdown(false);
    };

    const handleInputEditorClear = () => {
        if (!pathIsValid) {
            setValue("");
        }
    };

    return (
        <div className="ecc-auto-suggestion-box">
            <div className="ecc-auto-suggestion-box__editor-box">
                <CodeEditor
                    onChange={handleChange}
                    onCursorChange={handleCursorChange}
                    onSelection={handleInputEditorSelection}
                    value={value}
                />
                <div onClick={handleInputEditorClear}>
                    <Icon
                        className={`editor__icon ${
                            pathIsValid ? "confirm" : "clear"
                        }`}
                        name={pathIsValid ? "confirm" : "clear"}
                    />
                </div>
            </div>
            <div className="ecc-auto-suggestion-box__dropdown">
                {shouldShowDropdown ? (
                    <Dropdown
                        options={suggestions}
                        onChange={handleDropdownChange}
                        value={value}
                        placeholder="Select from suggestions"
                    />
                ) : null}
            </div>
        </div>
    );
};

export default AutoSuggestion;
