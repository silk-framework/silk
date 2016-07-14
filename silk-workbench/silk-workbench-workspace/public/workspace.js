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
$(document).ready(
  function(){
    reloadWorkspace();
  }
);

function newProject() {
  showDialog(baseUrl + '/workspace/dialogs/newproject');
}

function importProject() {
  showDialog(baseUrl + '/workspace/dialogs/importproject');
}

function importLinkSpec(project) {
  showDialog(baseUrl + '/workspace/dialogs/importlinkspec/' + project);
}

function exportProject(project) {
  window.location = baseUrl + '/workspace/projects/' + project + '/export'
}

function deleteProject(project) {
  deleteTaskConfirm(project, baseUrl + '/workspace/projects/' + project);
}

function executeProject(project) {
  showDialog(baseUrl + '/workspace/dialogs/executeProject/' + project);
}

function editPrefixes(project) {
  showDialog(baseUrl + '/workspace/dialogs/prefixes/' + project);
}

function editResources(project) {
  showDialog(baseUrl + '/workspace/dialogs/resources/' + project);
}

function reloadWorkspace() {
  $.get(baseUrl + "/workspace/tree", function(data) {
    $('#workspace_tree').html(data);
  }).fail(function(request) {
    alert("Error reloading workspace: " + request.responseText);
  });
}

function reloadWorkspaceInBackend() {
  $.post(baseUrl + '/workspace/reload', function(data) {
    reloadWorkspace();
  }).fail(function(request) {
    alert("Error reloading workspace: " + request.responseText);
  });
}

function workspaceDialog(relativePath) {
  showDialog(baseUrl + '/' + relativePath, reloadWorkspace);
}

function putTask(path, xml) {
  $.ajax({
    type: 'PUT',
    url: path,
    contentType: 'text/xml',
    processData: false,
    data: xml,
    success: function(data) {
      $('.dialog').dialog('close');
      reloadWorkspace();
    },
    error: function(request) {
      alert(request.responseText);
    }
  });
}

function deleteTask(path) {
  $.ajax({
    type: 'DELETE',
    url: path,
    success: function(data) {
      reloadWorkspace();
    },
    error: function(request) {
      alert("Error deleting:" + request.responseText);
    }
  });
}

function deleteTaskConfirm(name, path) {
  showDialog(baseUrl + '/workspace/dialogs/removeresource/' + name + "/" + encodeURIComponent(path));
}