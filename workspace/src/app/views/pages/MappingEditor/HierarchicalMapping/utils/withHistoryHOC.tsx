import React from "react";
import { useHistory } from "react-router";

/** Adds the history to a component. */
export const withHistoryHOC = (Component: any) => {
    return (props: any) => {
        const history = useHistory();
        return <Component history={history ?? window.history} {...props} />;
    };
};
