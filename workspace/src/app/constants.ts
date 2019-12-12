// @ts-ignore
export const HOST = window.DI.publicBaseUrl || process.env.HOST;
// @ts-ignore
export const API_ENDPOINT =  window.DI.basePath || process.env.API_ENDPOINT;

export const LEGACY_API_ENDPOINT =  process.env.LEGACY_API_ENDPOINT;

export const AUTH_ENDPOINT = HOST + '/oauth/authorize';

export const CLIENT_ID = 'eldsClient';
export const DEFAULT_LANG = 'en';

export const isDevelopment = process.env.NODE_ENV !== 'production';
