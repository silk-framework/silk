import React, { Suspense } from "react";
import { Route, Switch } from "react-router-dom";
import Loading from "./views/components/loading/Loading";
import { SERVE_PATH } from "./constants";
export default function RouterOutlet({routes}) {
    return (
        <Suspense fallback={<Loading/>}>
            <Switch>
                {
                    routes.map(route => {
                        return (
                            <Route
                                key={route.path as string}
                                path={`${SERVE_PATH}${route.path}`}
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
