import { FlowEditorState } from "../typings";

export function initialMainEditorState(): FlowEditorState {
    return {
        portsConfig: null,
        portsConfigLoading: false,
    };
}
