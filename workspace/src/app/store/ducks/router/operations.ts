import qs from 'query-string';
import { getLocation, push, replace } from "connected-react-router";
import { SERVE_PATH } from "../../../constants";

interface IQueryParams {
    [key: string]: any;
}

const setQueryString = (queryParams: IQueryParams) => {
    return (dispatch, getState) => {
        const location = getLocation(getState());
        const currentQuery = {};

        Object.keys(queryParams).map(paramName => {
            const values = queryParams[paramName];
            const validValue = Array.isArray(values) ? values : values.toString();

            if (validValue && validValue.length) {
                currentQuery[paramName] = validValue;
            } else {
                delete currentQuery[paramName];
            }
        });

        const qsStr = `${location.pathname}?${qs.stringify(currentQuery, {
            arrayFormat: "comma"
        })}`;
        dispatch(replace(qsStr));
    }
};

const goToPage = (path: string) => {
    return dispatch => {
        dispatch(push(SERVE_PATH + path))
    }
};


export default {
    setQueryString,
    goToPage
};
