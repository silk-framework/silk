// @ts-ignore
const {DI} = window;

export const HOST = DI.publicBaseUrl
    ? DI.publicBaseUrl + DI.basePath
    : process.env.HOST;

export const isDevelopment = process.env.NODE_ENV !== 'production';
export const API_ENDPOINT =  process.env.API_ENDPOINT;
export const LEGACY_API_ENDPOINT =  process.env.LEGACY_API_ENDPOINT;
export const AUTH_ENDPOINT = HOST + '/oauth/authorize';

export const CLIENT_ID = 'eldsClient';
export const DEFAULT_LANG = 'en';

export const DATA_TYPES = {
    PROJECT: 'project',
    DATASET: 'Dataset',
    TRANSFORM: 'Transform',
    LINKING: 'Linking',
    TASK: 'Task',
};
