import React from "react";
import { Controlled as ControlledEditor } from "react-codemirror2";
import { Classes as BlueprintClassNames } from "@blueprintjs/core";
import "codemirror/mode/sparql/sparql.js";
import CodeMirror from "codemirror";

export interface IEditorProps {
  // Is called with the editor instance that allows access via the CodeMirror API
  setEditorInstance: (editor: CodeMirror.Editor) => any
  // Called whenever the editor content changes
  onChange: (value: string) => any
  // Called when the cursor position changes
  onCursorChange: (pos: any, coords: any) => any
  // The editor theme, e.g. "sparql"
  mode?: string | null
  // The initial value of the editor
  initialValue: string
  // Called when the focus status changes
  onFocusChange: (focused: boolean) => any
  // Called when the user presses a key
  onKeyDown: (event: KeyboardEvent) => any
  // Called when the user selects text
  onSelection: (ranges: IRange[]) => any
  // If the <Tab> key is enabled as normal input, i.e. it won't have the behavior of changing to the next input element, expected in a web app.
  enableTab?: boolean
}

export interface IRange {
  from: number
  to: number
}

const SingleLineCodeEditor = ({
                                setEditorInstance,
                                onChange,
                                onCursorChange,
                                mode = null,
                                initialValue,
                                onFocusChange,
                                onKeyDown,
                                onSelection,
                                enableTab = false,
                              }: IEditorProps) => {
  return (
    <div className={"ecc-input-editor " + BlueprintClassNames.INPUT}>
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
            extraKeys: enableTab ? undefined : {Tab: false}
        }}
        onSelection={(editor, data) => {
          if(Array.isArray(data?.ranges)) {
            onSelection(data.ranges
                .map(r => ({from: r.from().ch, to: r.to().ch}))
                .filter(r => r.from != r.to))
          }
        }}
        onCursor={(editor, data) => {
          onCursorChange(data, editor.cursorCoords(true, "div"));
        }}
        onBeforeChange={(editor, data, value) => {
          const trimmedValue = value.replace(/\n/g, "");
          onChange(trimmedValue);
        }}
        onKeyDown={(_, event) => onKeyDown(event)}
      />
    </div>
  );
};

export default SingleLineCodeEditor;
