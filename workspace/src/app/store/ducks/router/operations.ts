import qs from "qs";
import { getLocation, push, replace } from "connected-react-router";
import { SERVE_PATH } from "../../../constants";

interface IQueryParams {
    [key: string]: any;
}

export interface IPageLabels {
    pageTitle?: string;
    projectLabel?: string;
    taskLabel?: string;
}

const setQueryString = (queryParams: IQueryParams) => {
    return (dispatch, getState) => {
        const location = getLocation(getState());
        const currentQuery = {};

        Object.keys(queryParams).forEach((paramName) => {
            const values = queryParams[paramName];
            const validValue = Array.isArray(values) ? values : values.toString();

            if (validValue && validValue.length) {
                currentQuery[paramName] = validValue;
            } else {
                delete currentQuery[paramName];
            }
        });

        const qsStr = `${location.pathname}?${qs.stringify(currentQuery, {
            arrayFormat: "comma",
        })}`;
        dispatch(push(qsStr));
    };
};

/**
 * Navigates to a specific page of the application.
 * @param path       The path of the page. This may be an absolute or relative path. If it starts with a '/' it will
 *                   be considered an absolute path, else it will be considered as relative to the '/workspace' path.
 * @param pageLabels The labels of the target page, e.g. page title and labels for parts of the breadcrumb.
 */
const goToPage = (path: string, pageLabels: IPageLabels) => {
    const isAbsolute = path.startsWith("/");
    return (dispatch) => {
        dispatch(push(isAbsolute ? path : SERVE_PATH + path, { pageLabels: pageLabels }));
    };
};

const replacePage = (path: string, pageLabels: IPageLabels) => {
    const isAbsolute = path.startsWith("/");
    return (dispatch) => {
        dispatch(replace(isAbsolute ? path : SERVE_PATH + path, { pageLabels: pageLabels }));
    };
};

export default {
    setQueryString,
    goToPage,
    replacePage,
};
