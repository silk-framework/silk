import { legacyApiEndpoint } from "./getApiEndpoint";

export const downloadResource = (projectId: string, typeId: string) => {
    window.open(legacyApiEndpoint(`/projects/${projectId}/export/${typeId}`));
};
