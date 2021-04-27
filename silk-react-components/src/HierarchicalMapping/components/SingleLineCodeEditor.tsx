import React from "react";
import { Controlled as ControlledEditor } from "react-codemirror2";
import "codemirror/mode/sparql/sparql.js";

const SingleLineCodeEditor = ({
  setEditorInstance,
  onChange,
  onCursorChange,
  mode = "sparql",
  initialValue,
  onFocusChange,
  handleSpecialKeysPress,
}) => {
  return (
    <div className="ecc-input-editor">
      <ControlledEditor
        editorDidMount={(editor) => {
          editor.on("beforeChange", (_, change) => {
            // Prevent the user from entering new-line characters, since this is supposed to be a one-line editor.
            const newText = change.text.join("").replace(/\n/g, "");
            //failing unexpectedly during undo and redo
            if (change.update && typeof change.update === "function") {
              change.update(change.from, change.to, [newText]);
            }
            return true;
          });
          setEditorInstance(editor);
        }}
        value={initialValue}
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
        onKeyDown={(_, event) => handleSpecialKeysPress(event)}
      />
    </div>
  );
};

export default SingleLineCodeEditor;
