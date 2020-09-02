import React, { Suspense } from "react";
import { Route, Switch } from "react-router-dom";
import Loading from "./views/shared/Loading";
import { getFullRoutePath } from "./utils/routerUtils";
import { AppLayout } from "./views/layout/AppLayout/AppLayout";
import { useTranslation } from "react-i18next";

export default function RouterOutlet({ routes }) {
    const [t] = useTranslation();
    return (
        <Suspense fallback={<Loading posGlobal description={t("common.app.loading", "Loading page.")} />}>
            <Switch>
                {routes.map((route) => {
                    return (
                        <Route key={route.path} path={getFullRoutePath(route.path)} exact={route.exact}>
                            <AppLayout>
                                <route.component />
                            </AppLayout>
                        </Route>
                    );
                })}
            </Switch>
        </Suspense>
    );
}
