// @ts-ignore
const { DI = {} } = window;

/**
 * The application public address
 * e.g. http://localhost:9000
 */
export const PUBLIC_URL = DI.publicBaseUrl || process.env.HOST;

/**
 * The application context
 * e.g. /dataintegration
 */
export const CONTEXT_PATH = DI.basePath || "";

/**
 * The path of new workspace
 */
export const SERVE_PATH = CONTEXT_PATH + "/workbench";

/**
 * Full hostname
 */
export const HOST = PUBLIC_URL + CONTEXT_PATH;

/**
 * check current environment
 */
export const isDevelopment = process.env.NODE_ENV !== "production";

export const isTestEnv = process.env.NODE_ENV === "test";

export const API_ENDPOINT = process.env.API_ENDPOINT ?? "/api";

export const AUTH_ENDPOINT = HOST + "/oauth/authorize";

/** Legacy UI */
(window as any).__DEBUG__ = false
