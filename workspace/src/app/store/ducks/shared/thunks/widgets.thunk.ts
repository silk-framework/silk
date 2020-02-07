import { getApiEndpoint } from "../../../../utils/getApiEndpoint";
import fetch from "../../../../services/fetch";

export const getProjectPrefixes = async (projectId: string): Promise<Object> => {
    const url = getApiEndpoint(`/projects/${projectId}/prefixes`);
    try {
        const {data} = await fetch({
            url
        });
        return data;
    } catch {}
};

export const addProjectPrefix = async (projectId: string, prefixName: string, prefixUri: string) => {
    const url = getApiEndpoint(`/projects/${projectId}/prefixes/${prefixName}`);
    try {
        const {data} = await fetch({
            url,
            method: 'PUT',
            body: JSON.stringify(prefixUri)
        });
        return data;
    } catch {

    }
};

export const removeProjectPrefixes = async (projectId: string, prefixName: string) => {
    const url = getApiEndpoint(`/projects/${projectId}/prefixes/${prefixName}`);
    try {
        const {data} = await fetch({
            url,
            method: 'DELETE'
        });
        return data;
    } catch {

    }
};
