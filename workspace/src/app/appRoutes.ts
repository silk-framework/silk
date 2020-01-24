import { lazy } from "react";
import { RouteProps } from "react-router";

const DashboardPage = lazy(() => import('./views/pages/Workspace'));
const DatasetPage = lazy(() => import('./views/pages/Dataset'));

const appRoutes: RouteProps[] = [
    {
        path: "/",
        exact: true,
        component: DashboardPage,
    },
    {
        path: "/projects/:projectId",
        component: DashboardPage,
        exact: true
    },
    {
        path: "/datasets/:datasetId",
        component: DatasetPage,
        exact: true
    },
];

export default appRoutes;
