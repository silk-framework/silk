// @ts-ignore
const {DI = {}} = window;

export const BASE_PATH = DI.basePath || '';
export const SERVE_PATH = BASE_PATH + '/workspaceNew';

export const HOST = DI.publicBaseUrl
    ? DI.publicBaseUrl + BASE_PATH
    : process.env.HOST;

export const isDevelopment = process.env.NODE_ENV !== 'production';
export const API_ENDPOINT =  process.env.API_ENDPOINT;
export const AUTH_ENDPOINT = HOST + '/oauth/authorize';

export const CLIENT_ID = 'eldsClient';
export const DEFAULT_LANG = 'en';

export const DATA_TYPES = {
    PROJECT: 'project',
    DATASET: 'dataset',
    WORKFLOW: 'workflow',
    TRANSFORM: 'transform',
    LINKING: 'Linking',
    TASK: 'Task',
};

export const INPUT_VALID_TYPES = {
    BOOLEAN: 'boolean',
    TEXTAREA: 'textarea',
    INTEGER: 'int',
    MULTILINE_STRING: 'multiline string',
    STRING: 'string',
    OPTION_INT: 'option[int]',
    ENUMERATION: 'enumeration',
    PASSWORD: 'password',
    RESOURCE: 'resource',
};
