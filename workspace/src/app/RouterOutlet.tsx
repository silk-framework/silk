import React, { Suspense } from "react";
import { Route, Switch } from "react-router-dom";
import Loading from "./views/shared/Loading";
import { getFullRoutePath } from "./utils/routerUtils";

export default function RouterOutlet({routes}) {
    return (
        <Suspense fallback={<Loading/>}>
            <Switch>
                {
                    routes.map(route => {
                        return (
                            <Route
                                key={route.path}
                                path={getFullRoutePath(route.path)}
                                exact={route.exact}
                                component={route.component}
                            />
                        )
                    })
                }
            </Switch>
        </Suspense>
    )
}
