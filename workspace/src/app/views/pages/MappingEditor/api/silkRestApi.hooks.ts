/** Returns frontend init data or undefined if it has not been fetched, yet. */
import { useEffect, useState } from "react";
import silkApi from "./silkRestApi";
import { setDefaultProjectPageSuffix } from "../../../../utils/routerUtils";
import {IInitFrontend} from "@ducks/common/typings";

interface IProps {
    errorStatus: number;
    error: any;
}

type ErrorHandler = (data: IProps) => any;

/** Hook that provides the frontend init data. */
export const useInitFrontend = (errorHandler?: ErrorHandler): IInitFrontend | undefined => {
    const [frontendData, setFrontendData] = useState<IInitFrontend | undefined>(undefined);

    useEffect(() => {
        silkApi
            .initFrontendInfo()
            .then(({ data }) => {
                if (data.defaultProjectPagePrefix) {
                    setDefaultProjectPageSuffix(data.defaultProjectPagePrefix);
                }
                setFrontendData(data);
            })
            .catch(({ status, data }) => {
                if (errorHandler) {
                    // No meaningful default value, let called handle the error.
                    errorHandler({ error: data, errorStatus: status });
                }
            });
    }, []);

    return frontendData;
};
