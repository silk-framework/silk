import React, { useEffect } from "react";
import { ConnectedRouter } from "connected-react-router";
import { useDispatch } from "react-redux";

import { commonOp } from "@ducks/common";
import RouterOutlet from "./RouterOutlet";
import { getHistory } from "./store/configureStore";
import { IRouteProps } from "./appRoutes";
import { useGlobalAppDragMonitor } from "./hooks/useGlobalAppDragMonitor";

interface IProps {
    routes: IRouteProps[];
    externalRoutes: any;
}

export default function App({ externalRoutes, routes }: IProps) {
    const dispatch = useDispatch();
    useGlobalAppDragMonitor();

    useEffect(() => {
        dispatch(commonOp.fetchCommonSettingsAsync());
        dispatch(commonOp.fetchExportTypesAsync());
    }, [commonOp]);

    return (
        <ConnectedRouter history={getHistory()}>
            <RouterOutlet routes={routes} />
        </ConnectedRouter>
    );
}
