export const HOST = process.env.HOST;
export const API_ENDPOINT = HOST + process.env.API_ENDPOINT;
export const AUTH_ENDPOINT = HOST + '/oauth/authorize';

export const CLIENT_ID = 'eldsClient';
export const DEFAULT_LANG = 'en';

export const isDevelopment = process.env.NODE_ENV !== 'production';
