import React, { Suspense } from "react";
import { Route, Switch } from "react-router-dom";
import Loading from "./views/shared/Loading";
import { getFullRoutePath } from "./utils/routerUtils";
import { AppLayout } from "./views/layout/AppLayout/AppLayout";
import { useTranslation } from "react-i18next";
import { IRouteProps } from "./appRoutes";
import { ApplicationContainer, ApplicationContent } from "@eccenca/gui-elements";

interface RouterOutletProps {
    routes: IRouteProps[];
}

export default function RouterOutlet({ routes }: RouterOutletProps) {
    const [t] = useTranslation();
    return (
        <Suspense fallback={<Loading posGlobal description={t("common.app.loading", "Loading page.")} />}>
            <Switch>
                {routes.map((route) => {
                    const Component = route.component as any;
                    return (
                        <Route key={route.path} path={getFullRoutePath(route.path)} exact={route.exact}>
                            {route.componentOnly && Component ? (
                                <ApplicationContainer monitorDropzonesFor={["application/reactflow", "Files"]}>
                                    <ApplicationContent>
                                        <Component />
                                    </ApplicationContent>
                                </ApplicationContainer>
                            ) : (
                                <AppLayout>{Component && <Component />}</AppLayout>
                            )}
                        </Route>
                    );
                })}
            </Switch>
        </Suspense>
    );
}
