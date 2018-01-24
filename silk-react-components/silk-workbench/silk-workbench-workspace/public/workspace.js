/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// -- init
$(document).ready(function() {
    reloadWorkspace();
});

/* exported newProject
silk-workbench/silk-workbench-workspace/app/views/workspace/workspace.scala.html
*/
function newProject() {
    showDialog(`${baseUrl}/workspace/dialogs/newproject`);
}

/* exported importProject
silk-workbench/silk-workbench-workspace/app/views/workspace/workspace.scala.html
*/
function importProject() {
    showDialog(`${baseUrl}/workspace/dialogs/importproject`);
}

/* exported cloneProject
silk-workbench/silk-workbench-workspace/app/views/workspace/workspaceTree.scala.html
*/
function cloneProject(project) {
    showDialog(`${baseUrl}/workspace/dialogs/cloneProject?project=${project}`);
}

/* exported cloneTask
silk-workbench/silk-workbench-workspace/app/views/workspace/workspaceTree.scala.html
*/
function cloneTask(project, task) {
    showDialog(
        `${baseUrl}/workspace/dialogs/cloneTask?project=${project}&task=${task}`
    );
}

/* exported importLinkSpec
silk-workbench/silk-workbench-workspace/app/views/workspace/workspaceTree.scala.html
*/
function importLinkSpec(project) {
    showDialog(`${baseUrl}/workspace/dialogs/importlinkspec/${project}`);
}

/* exported exportProject
silk-workbench/silk-workbench-workspace/app/views/workspace/workspaceTree.scala.html
*/
function exportProject(project) {
    window.location = `${baseUrl}/workspace/projects/${project}/export`;
}

/* exported executeProject
silk-workbench/silk-workbench-workspace/app/views/workspace/workspaceTree.scala.html
*/
function executeProject(project) {
    showDialog(`${baseUrl}/workspace/dialogs/executeProject/${project}`);
}

/* exported editPrefixes
silk-workbench/silk-workbench-workspace/app/views/workspace/workspaceTree.scala.html
*/
function editPrefixes(project) {
    showDialog(`${baseUrl}/workspace/dialogs/prefixes/${project}`);
}

/* exported editResources
silk-workbench/silk-workbench-workspace/app/views/workspace/workspaceTree.scala.html
*/
function editResources(project) {
    showDialog(`${baseUrl}/workspace/dialogs/resources/${project}`);
}

/* exported reloadWorkspace
silk-workbench/silk-workbench-workspace/app/views/workspace/cloneProjectDialog.scala.html
silk-workbench/silk-workbench-workspace/app/views/workspace/cloneTaskDialog.scala.html
silk-workbench/silk-workbench-workspace/app/views/workspace/importLinkSpecDialog.scala.html
silk-workbench/silk-workbench-workspace/app/views/workspace/importProjectDialog.scala.html
silk-workbench/silk-workbench-rules/app/views/dialogs/linkingTaskDialog.scala.html
silk-workbench/silk-workbench-workspace/app/views/workspace/newProjectDialog.scala.html
silk-workbench/silk-workbench-rules/app/views/dialogs/transformationTaskDialog.scala.html
silk-workbench/silk-workbench-workflow/app/views/workflow/workflowTaskDialog.scala.html
*/
function reloadWorkspace() {
    $.get(`${baseUrl}/workspace/tree`, function(data) {
        $('#workspace_tree').html(data);
    }).fail(function(request) {
        alert(`Error reloading workspace: ${request.responseText}`);
    });
}

/* exported reloadWorkspaceInBackend
silk-workbench/silk-workbench-workspace/app/views/workspace/workspace.scala.html
 */
function reloadWorkspaceInBackend() {
    $.post(`${baseUrl}/workspace/reload`, function() {
        reloadWorkspace();
    }).fail(function(request) {
        alert(`Error reloading workspace: ${request.responseText}`);
    });
}

/* exported workspaceDialog
silk-workbench/silk-workbench-workspace/app/views/workspace/workspaceTree.scala.html
 */
function workspaceDialog(relativePath) {
    showDialog(`${baseUrl}/${relativePath}`);
}

/* exported putTask
silk-workbench/silk-workbench-workspace/app/views/workspace/customTask/customTaskDialog.scala.html
silk-workbench/silk-workbench-workspace/app/views/workspace/dataset/datasetDialog.scala.html
 */
function putTask(
    project,
    task,
    json,
    callbacks = {
        success() {},
        error() {},
    }
) {
    $.ajax({
        type: 'PATCH',
        url: `${baseUrl}/workspace/projects/${project}/tasks/${task}`,
        contentType: 'application/json;charset=UTF-8',
        processData: false,
        data: JSON.stringify(json),
        dataType: 'json',
        error(request) {
            const responseJson = JSON.parse(request.responseText);
            var responseMessage = responseJson.message; // Old format
            if (responseMessage === undefined) {
                if (responseJson.title === 'Bad Request') {
                    responseMessage = 'Task could not be saved! Details: ';
                } else {
                    responseMessage = '';
                }
                var finestDetail = responseJson;
                while (finestDetail.cause !== null) {
                    finestDetail = finestDetail.cause;
                }
                responseMessage = `${responseMessage +
                    finestDetail.title}: ${finestDetail.detail}`;
            }
            callbacks.error(responseMessage);
        },
        success() {
            reloadWorkspace();
            callbacks.success();
        },
    });
}

function postTask(
    project,
    json,
    callbacks = {
      success() {},
      error() {},
    }
) {
  $.ajax({
    type: 'POST',
    url: `${baseUrl}/workspace/projects/${project}/tasks`,
    contentType: 'application/json;charset=UTF-8',
    processData: false,
    data: JSON.stringify(json),
    dataType: 'json',
    error(request) {
      const responseJson = JSON.parse(request.responseText);
      var responseMessage = responseJson.message; // Old format
      if (responseMessage === undefined) {
        if (responseJson.title === 'Bad Request') {
          responseMessage = 'Task could not be saved! Details: ';
        } else {
          responseMessage = '';
        }
        var finestDetail = responseJson;
        while (finestDetail.cause !== null) {
          finestDetail = finestDetail.cause;
        }
        responseMessage = `${responseMessage +
        finestDetail.title}: ${finestDetail.detail}`;
      }
      callbacks.error(responseMessage);
    },
    success() {
      reloadWorkspace();
      callbacks.success();
    },
  });
}

/* exported deleteProject
silk-workbench/silk-workbench-workspace/app/views/workspace/removeProjectDialog.scala.html
 */
function deleteProject(project) {
    $.ajax({
        type: 'DELETE',
        url: `${baseUrl}/workspace/projects/${project}`,
        success() {
            reloadWorkspace();
        },
        error(request) {
            alert(`Error deleting:${request.responseText}`);
        },
    });
}

/* exported deleteTask
silk-workbench/silk-workbench-workspace/app/views/workspace/removeTaskDialog.scala.html
 */
function deleteTask(project, task) {
    $.ajax({
        type: 'DELETE',
        url: `${baseUrl}/workspace/projects/${project}/tasks/${task}`,
        success() {
            reloadWorkspace();
        },
        error(request) {
            alert(`Error deleting:${request.responseText}`);
        },
    });
}

/* exported deleteProjectConfirm
silk-workbench/silk-workbench-workspace/app/views/workspace/workspaceTree.scala.html
 */
function deleteProjectConfirm(project) {
    showDialog(`${baseUrl}/workspace/dialogs/removeproject/${project}`);
}

/* exported deleteTaskConfirm
silk-workbench/silk-workbench-workspace/app/views/workspace/workspaceTree.scala.html
 */
function deleteTaskConfirm(project, task) {
    showDialog(`${baseUrl}/workspace/dialogs/removetask/${project}/${task}`);
}

/* exported deleteResourceConfirm
silk-workbench/silk-workbench-workspace/app/views/workspace/resourcesDialog.scala.html
 */
function deleteResourceConfirm(name, path) {
    showDialog(
        `${baseUrl}/workspace/dialogs/removeresource/${name}?path=${encodeURIComponent(
            path
        )}`,
        'secondary'
    );
}
