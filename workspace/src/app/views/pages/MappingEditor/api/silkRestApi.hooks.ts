/** Returns frontend init data or undefined if it has not been fetched, yet. */
import { useEffect, useState } from "react";
import silkApi from "./silkRestApi";

/** Config information from the backend to initialize the frontend. */
export interface IInitFrontend {
    /**
     * If the workspace if currently empty, i.e. has no projects in it.
     */
    emptyWorkspace: boolean;
    /**
     * Initial language from backend. This can be "overwritten" by the user via the UI.
     */
    initialLanguage: string;

    /** The max. file upload size in bytes supported by the backend. */
    maxFileUploadSize?: number;

    /**
     * DM url, in case of missing, hide navigation bar
     */
    dmBaseUrl?: string;

    /** The URI of the logged-in user. */
    userUri?: string;
}

interface IProps {
    errorStatus: number;
    error: any;
}

type ErrorHandler = (data: IProps) => any;

/** Hook that provides the frontend init data. */
export const useInitFrontend = (errorHandler?: ErrorHandler) => {
    const [frontendData, setFrontendData] = useState<IInitFrontend | undefined>(undefined);

    useEffect(() => {
        silkApi
            .initFrontendInfo()
            .then(({ data }) => setFrontendData(data))
            .catch(({ status, data }) => {
                if (errorHandler) {
                    // No meaningful default value, let called handle the error.
                    errorHandler({ error: data, errorStatus: status });
                }
            });
    }, []);

    return frontendData;
};
