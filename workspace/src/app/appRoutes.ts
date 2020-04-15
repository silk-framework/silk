import { lazy } from "react";
import { RouteProps } from "react-router";

const DashboardPage = lazy(() => import('./views/pages/Workspace'));
const DatasetPage = lazy(() => import('./views/pages/Dataset'));
const WorkflowPage = lazy(() => import('./views/pages/Workflow'));
const TransformPage = lazy(() => import('./views/pages/Transform'));
const LinkingPage = lazy(() => import('./views/pages/Linking'));
const ScriptTaskPage = lazy(() => import('./views/pages/ScriptTask'));

interface IRouteProps extends RouteProps {
    path: string
};

const appRoutes: IRouteProps[] = [
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
        path: "/projects/:projectId/dataset/:taskId",
        component: DatasetPage,
        exact: true
    },
    {
        path: "/projects/:projectId/workflow/:taskId",
        component: WorkflowPage,
        exact: true
    },
    {
        path: "/projects/:projectId/transform/:taskId",
        component: TransformPage,
        exact: true
    },
    {
        path: "/projects/:projectId/linking/:taskId",
        component: LinkingPage,
        exact: true
    },
    {
        path: "/projects/:projectId/task/:taskId",
        component: ScriptTaskPage,
        exact: true
    },
];

export default appRoutes;
