import { PortConfigResponse } from ".";

export interface FlowEditorState {
    /** Contains all possible port configurations for different tasks type **/
    portsConfig: PortConfigResponse | null;

    /** state that is toggled if, or not, pending requests exists based on ports config request **/
    portsConfigLoading: boolean;
}
