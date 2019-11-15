export const DM_ENDPOINT = process.env.DM_ENDPOINT;
export const SPARQL_DEFAULT_ENDPOINT = DM_ENDPOINT + '/proxy/default/sparql';
export const AUTH_ENDPOINT = DM_ENDPOINT + '/oauth/authorize';

export const CLIENT_ID = 'eldsClient';
export const DEFAULT_LANG = 'en';

export const isDevelopment = process.env.NODE_ENV !== 'production';
