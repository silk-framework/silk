import { getApiEndpoint } from "../../../../utils/getApiEndpoint";
import fetch from "../../../../services/fetch";


export const getProjectPrefixes = async (projectId: string): Promise<Object> => {
    const url = getApiEndpoint(`/projects/${projectId}/prefixes`);
    const {data} = await fetch({
        url
    });
    return data;
};
