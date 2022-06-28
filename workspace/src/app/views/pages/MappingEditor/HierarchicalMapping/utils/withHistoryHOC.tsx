import {useHistory} from "react-router";
import React from "react";

/** Adds the history to a component. */
export const withHistoryHOC = (Component: any) => {
    return (props: any) => {
        const screenWidth = useHistory()

        return <Component width={screenWidth} {...props} />;
    };
};
