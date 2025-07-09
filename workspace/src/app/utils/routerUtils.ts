import { SERVE_PATH } from "../constants/path";

export const getFullRoutePath = (path: string) => `${SERVE_PATH}${path}`;

let defaultProjectPagePrefix = "";
export const setDefaultProjectPageSuffix = (suffix: string) => {
    defaultProjectPagePrefix = suffix;
};

// The project path
export const absoluteProjectPath = (projectId: string) =>
    `${SERVE_PATH}/projects/${projectId}${defaultProjectPagePrefix}`;
