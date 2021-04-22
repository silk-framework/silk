import React from "react";
import { Controlled as ControlledEditor } from "react-codemirror2";
import "codemirror/mode/sparql/sparql.js";
import PropTypes from "prop-types";

export function CodeEditor({
    setEditorInstance,
    onChange,
    onCursorChange,
    mode = "sparql",
    value,
    onFocusChange,
}) {
    return (
        <div className="ecc-input-editor">
            <ControlledEditor
                editorDidMount={(editor) => {
                    editor.on("beforeChange", (_, change) => {
                        console.log({ change });
                        var newtext = change.text.join("").replace(/\n/g, "");
                        //failing unexpectedly during undo and redo
                        if (
                            change.update &&
                            typeof change.update === "function"
                        ) {
                            change.update(change.from, change.to, [newtext]);
                        }
                        return true;
                    });
                    setEditorInstance(editor);
                }}
                value={value}
                onFocus={() => onFocusChange(true)}
                onBlur={() => onFocusChange(false)}
                options={{
                    mode: mode,
                    lineNumbers: false,
                    theme: "xq-light",
                }}
                onCursor={(editor, data) => {
                    onCursorChange(data, editor.cursorCoords(true, "div"));
                }}
                onBeforeChange={(editor, data, value) => {
                    const trimmedValue = value.replace(/\n/g, "");
                    onChange(trimmedValue);
                }}
            />
        </div>
    );
}

CodeEditor.propTypes = {
    mode: PropTypes.string,
    value: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    onCursorChange: PropTypes.func.isRequired,
    onSelection: PropTypes.func,
};
