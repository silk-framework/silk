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

Each task may provide one or more activities. An activity is a unit of work that can be executed in the background.

| Resource | Description |
| --- | --- |
| `POST activities/start | Starts a task activity. |
| `POST activities/cancel | Cancels a task activity. |

All resources support three parameters:
- `project`: The project name
- `task`: The task name
- `activity`: The name of the activity

Example for starting the `Generate Links` activity for the LinkMovies task:

    POST activities/start?project=MyProject&task=LinkMovies&activity=Generate%20Links` 
    