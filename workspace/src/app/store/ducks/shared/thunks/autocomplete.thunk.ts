import { workspaceApi } from "../../../../utils/getApiEndpoint";
import fetch from "../../../../services/fetch";

interface IPayload {
    pluginId: string;
    parameterId: string;
    projectId: string;
    dependsOnParameterValues: string[];
    textQuery: string;
}
export const getAutocompleteResultsAsync = async (payload: IPayload) => {
    let url = workspaceApi(`/pluginParameterAutoCompletion`);
    try {
        const {data} = await fetch({
            url,
            method: "POST",
            body: {
                ...payload,
                limit: 10000,
                offset: 0
            },
        });
        return data;
    } catch(e) {
        return e;
    }
};
