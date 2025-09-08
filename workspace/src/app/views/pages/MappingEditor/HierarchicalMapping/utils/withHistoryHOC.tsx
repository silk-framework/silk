import { useNavigate } from "react-router";
import React from "react";

/** Adds the history to a component. */
export const withHistoryHOC = (Component: any) => {
    return (props: any) => {
        const navigate = useNavigate();
        return <Component history={history ?? window.history} {...props} />;
    };
};
