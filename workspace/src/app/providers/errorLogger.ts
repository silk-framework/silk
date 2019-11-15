import { ErrorInfo } from "react";
import Dexie from 'dexie';
import { AxiosError } from "axios";

interface IClientInfo {
    language: string;
    platform: string;
    userAgent: string;
    vendor: string;
    url: string;
    screen: {
        height: number;
        width: number;
    }
}

interface INetworkError {
    url: string;
    status: number;
    payload?: any;
    headers?: any;
}

interface IError {
    name: string;
    message: string;
    client: IClientInfo;
    stack?: string;
    network?: INetworkError;
}

const LOG_TABLE = 'logs';

const tableInstance: any = new Dexie(LOG_TABLE);
tableInstance.version(1)
    .stores({
        [LOG_TABLE]: '++id, name, message, stack, client, react_stack, network',
    });

//@TODO: Periodically clear the logs table

/**
 * Using in window.onerror => global.ts
 * @desc Override the default onerror listener
 * @param message
 * @param url
 * @param lineNumber
 * @param colNo
 * @param error
 */
const onErrorHandler = (
    message: string,
    url: string,
    lineNumber: number,
    colNo?: number,
    error?: Error
): boolean => {
    if (error) {
        logError(error);
    } else {
        logError({
            name: message.split(':')[0],
            message,
            stack: `${url}:${lineNumber}:${colNo}`
        })
    }
    return true;
};

const isNetworkError = (err: AxiosError | Error): boolean => {
    if (!err || !Object.keys(err).length) {
        return false;
    }
    return 'response' in err;
};

/**
 * Collect all client info from browser
 */
const getClientInfo = (): IClientInfo => {
    const { navigator, location, screen } = window;
    const { language, platform, userAgent, vendor } = navigator;
    return {
        language,
        platform,
        userAgent,
        vendor,
        url: location.href,
        screen: {
            height: screen.height,
            width: screen.width,
        }
    }
};

/**
 * Generate the network error object
 * @param url
 * @param status
 * @param payload
 * @param headers
 * @return INetworkError
 */
const generateNetworkError = (
    url:string,
    status:number,
    payload?: any,
    headers?: any
): INetworkError => {
    const netErr: INetworkError = {
        url,
        status,
    };
    if (payload) {
        netErr.payload = payload;
    }
    if (headers && Object.keys(headers).length) {
        netErr.headers = headers;
    }
    return netErr;
};

/**
 * Generate the default js Error object
 * @param name
 * @param message
 * @param stack
 * @return IError
 */
const generateDefaultError = (
    name: string = 'DEFAULT_ERROR',
    message: string = 'DEFAULT_ERROR_MESSAGE',
    stack?: string
): Error => {
    const err: Error = {
        name,
        message,
    };
    if (stack) {
        err.stack = stack
    }
    return err;
};

/**
 * Send the error
 * @param error
 * @param reactErrorInfo
 */
const logError = async (
    error: AxiosError | Error,
    reactErrorInfo?: ErrorInfo
): Promise<boolean> => {
    let err: IError;

    if (isNetworkError(error)) {
        const { response } = error as AxiosError;
        const { title, detail, status } = response.data;
        const { headers, data, url } = response.config;
        err = {
            ...generateDefaultError(title, detail),
            client: getClientInfo(),
            network: generateNetworkError(url, status, data, headers)
        }

    } else if (error instanceof Error) {
        const { name, message, stack } = error;
        const newStack = reactErrorInfo ? reactErrorInfo.componentStack : stack;
        err = {
            ...generateDefaultError(name, message, newStack),
            client: getClientInfo(),
        };
    } else {
        console.warn('Uncaught Error type received ', error);
        return false;
    }

    await sendError(err);
    return true;
};

/**
 * Send the error via http or store in indexedDB
 * @TODO: remove from table if network available
 * @param error
 */
const sendError = async (error: IError) => {
    await tableInstance[LOG_TABLE].put(error);

};

export {
    logError,
    onErrorHandler
}
