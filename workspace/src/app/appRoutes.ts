import { lazy } from "react";
import { RouteProps } from "react-router";

const DashboardPage = lazy(() => import('./views/pages/dashboard/Dashboard'));

const appRoutes: RouteProps[] = [
    {
        path: "/",
        component: DashboardPage,
    },
];

export default appRoutes;
