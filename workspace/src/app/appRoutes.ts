import { lazy } from "react";
import { RouteProps } from "react-router";

const DashboardPage = lazy(() => import('./views/pages/Workspace'));
const DatasetPage = lazy(() => import('./views/pages/Dataset'));
const WorkflowPage = lazy(() => import('./views/pages/Workflow'));
const TransformPage = lazy(() => import('./views/pages/Transform'));
const LinkingPage = lazy(() => import('./views/pages/Linking'));
const ScriptTaskPage = lazy(() => import('./views/pages/ScriptTask'));

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
        path: "/projects/:projectId/dataset/:datasetId",
        component: DatasetPage,
        exact: true
    },
    {
        path: "/projects/:projectId/workflow/:workflowId",
        component: WorkflowPage,
        exact: true
    },
    {
        path: "/projects/:projectId/transform/:transformId",
        component: TransformPage,
        exact: true
    },
    {
        path: "/projects/:projectId/linking/:linkingId",
        component: LinkingPage,
        exact: true
    },
    {
        path: "/projects/:projectId/task/:scriptTaskId",
        component: ScriptTaskPage,
        exact: true
    },
];

export default appRoutes;
