# REST Interface

The Workbench provides a REST API.

## Workspace

All resources below are available under the path `{deploymentURI}/workspace/`.

### Manage Projects

| Resource | Description |
| --- | --- |
| `GET projects`              | Retrieves a JSON listing all projects in the workspace and their tasks by type |
| `PUT projects/<project>`    | Adds a new empty project |   
| `DELETE projects/<project>` | Delets an existing project |

### Resources

Each project may contain a number of resources (i.e. files). 
Resources may be referenced by tasks in the workspace.
For instance, a dataset may read a CSV-file or a linkage rule may read a stopword list.

| Resource | Description |
| --- | --- |
| `GET projects/<project>/resources ` | Retrieves a JSON listing all resources in a project. |
| `GET projects/<project>/resources/<name> ` | Retrieves a specific resource. |   
| `PUT projects/<project>/resources/<name> ` | Uploads a specific resource. |   
| `DELETE projects/<project>/resources/<name> ` | Deletes a specific resource. |   

### Datasets

A dataset description holds all properties needed to read entities from a dataset.
The dataset may either be local (e.g., a resource) or remote (e.g., accessed through queries).

| Resource | Description |
| --- | --- |
| `GET projects/<project>/datasets/<name> ` | Retrieves the properties of a specific dataset. |   
| `PUT projects/<project>/datasets/<name> ` | Creates or updates a dataset. |   
| `DELETE projects/<project>/datasets/<name> ` | Deletes a dataset. |   

### Start/Stop Activities

Each task may provide one or more activities. An activity is a unit of work that can be executed in the background.

| Resource | Description |
| --- | --- |
| `POST activities/start` | Starts an activity. |
| `POST activities/cancel` | Cancels an activity. |
| `GET activities/config` | Retrieves the configuration of an activity as key-value pairs. |
| `POST activities/config` | Updates the configuration of an activity. |
| `GET activities/status` | Retrieves the status of an activity. |
| `GET activities/updates` | Retrieves a Comet stream of Javascript calls to updateStatus whenever |
|                          | the status changes. Can be used to avoid status polling. |

All resources support three parameters:
- `project`: The project name.
- `task`: The task name. If left empty, a project activity is retrieved.
- `activity`: The name of the activity.

Example for starting the `Generate Links` activity for the LinkMovies task:

    POST activities/start?project=MyProject&task=LinkMovies&activity=Generate%20Links
    
    