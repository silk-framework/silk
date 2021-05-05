import React from "react";
import "@testing-library/jest-dom";
import { render } from "@testing-library/react";
import SingleLineEditor, {
    IEditorProps,
} from "../../../src/HierarchicalMapping/components/SingleLineCodeEditor";
import CodeMirror from "codemirror";

let props: IEditorProps,
    codeMirrorEditorInstance: CodeMirror.Editor = null;

describe("SingleLineCodeEditor", () => {
    beforeEach(() => {
        props = {
            setEditorInstance: jest.fn((editor) => {
                codeMirrorEditorInstance = editor;
            }),
            onChange: jest.fn((value) => {}),
            onCursorChange: jest.fn((pos, coords) => {}),
            mode: null,
            initialValue: "",
            onFocusChange: jest.fn((focused) => {}),
            onKeyDown: jest.fn((event) => {}),
            onSelection: jest.fn((ranges) => {}),
        };
    });

    it("should render properly", () => {
        const { container } = render(<SingleLineEditor {...props} />);
        expect(container.querySelector(".ecc-input-editor")).not.toBeNull();
    });

    it("should set the editorInstance immediately it's mounted", () => {
        render(<SingleLineEditor {...props} />);
        expect(props.setEditorInstance).toHaveBeenCalledTimes(1);
        expect(codeMirrorEditorInstance).not.toBeNull();
    });

    it("should set the default value on the editor input", () => {
        props = {
            ...props,
            initialValue: "This is the initial input",
        };
        const { getByText } = render(<SingleLineEditor {...props} />);
        expect(codeMirrorEditorInstance.getValue()).toBe(props.initialValue);
        expect(getByText(props.initialValue)).toBeTruthy();
    });

    it("should not allow user to create new lines", () => {
        render(<SingleLineEditor {...props} />);
        codeMirrorEditorInstance
            .getDoc()
            .setValue("I'm entering a new line \n character");
        expect(codeMirrorEditorInstance.lineCount()).toBe(1);
    });

    afterAll(() => {
        (props = null), (codeMirrorEditorInstance = null);
    });
});
