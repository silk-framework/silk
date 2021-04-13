import { legacyApiEndpoint } from "./getApiEndpoint";

export const downloadProject = (projectId: string, typeId: string) => {
    window.open(legacyApiEndpoint(`/projects/${projectId}/export/${typeId}`));
};
