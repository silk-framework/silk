# REST Interface

## Workspace

All resources below are available under the path `{deploymentURI}/workspace/`.

### Manage Projects

| Resource | Description |
| --- | --- |
| `GET projects`              | Retrieves a JSON listing all projects in the workspace and their tasks by type |
| `PUT projects/<project>`    | Adds a new empty project |   
| `DELETE projects/<project>` | Delets an existing project |

### Start/Stop Activities

| Resource | Description |
| --- | --- |
| `POST activities/start?project=<>&task=<>&activity=<>` | Starts a task activity. |
| `POST activities/cancel?project=<>&task=<>&activity=<>` | Cancels a task activity. |