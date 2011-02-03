function addLeaf(leaf, parent, desc)
{
                  if (leaf){
                     var leaf_ul = document.createElement("ul");
                         parent.appendChild(leaf_ul);
                     var leaf_li = document.createElement("li");
                         leaf_ul.appendChild(leaf_li);
                     var leaf_span = document.createElement("span");
                         leaf_span.setAttribute('class', 'file');
                         leaf_span.innerHTML = desc+leaf;
                         leaf_li.appendChild(leaf_span);
                  }
}

// display the workspace as treeview
function updateWorkspace(workspaceVar){
        //var obj = jQuery.parseJSON('{"workspace": {   "project": [ {"name": "Project_1", "dataSource":[ {"name":"KEGG","url":"http://kegg.us"},{"name":"Wiki","url":"http://aba.com"}  ],  "linkingTask":[ {"name":"Gene","source":"KEGG","target":"Wiki","targetDataset":"rdf:type Wiki:Gene","sourceDataset":"rdf:type KEGG:gene"},{"name":"linkTask2","source":"KEGG","target":"Wiki"} ] }, {"name": "Project_2", "dataSource":[ {"name":"ABA","url":"http://aba.com"} ] }     ] } }');
        var obj = workspaceVar;

        var tree = document.createElement("div");
            tree.id = "div_tree";

        var root = document.createElement("ul");
            root.id = 'tree';
            root.setAttribute('class','filetree');
            tree.appendChild(root);

        // for each project
        for (var p in obj.workspace.project) {
           var  proj = document.createElement("li");
                proj.setAttribute('class', 'closed');
                root.appendChild(proj);
                var proj_span = document.createElement("span");
                    proj_span.setAttribute('class', 'folder');
                    proj_span.innerHTML = obj.workspace.project[p].name;
                    proj.appendChild(proj_span);

             // display dataSource
            for (var d in obj.workspace.project[p].dataSource){
                var ds_name_ul = document.createElement("ul");
                    proj.appendChild(ds_name_ul);
                var ds_name_li = document.createElement("li");
                    ds_name_li.setAttribute('class', 'closed');
                    ds_name_ul.appendChild(ds_name_li);
                var ds_name_span = document.createElement("span");
                    ds_name_span.setAttribute('class', 'folder');
                    ds_name_span.innerHTML = 'Data Source: '+obj.workspace.project[p].dataSource[d].name;
                    ds_name_li.appendChild(ds_name_span);

                addLeaf(obj.workspace.project[p].dataSource[d].url,ds_name_li, 'url: ');
            }

            // display linkingTask
            for (var l in obj.workspace.project[p].linkingTask){
                var lt_name_ul = document.createElement("ul");
                    proj.appendChild(lt_name_ul);
                var lt_name_li = document.createElement("li");
                    lt_name_li.setAttribute('class', 'closed');
                    lt_name_ul.appendChild(lt_name_li);
                var lt_name_a = document.createElement("a");
                    lt_name_a.href = 'linkSpec?'+obj.workspace.project[p].name +':'+obj.workspace.project[p].linkingTask[l].name;
                    lt_name_li.appendChild(lt_name_a);
                var lt_name_span = document.createElement("span");
                    lt_name_span.setAttribute('class', 'folder');
                    lt_name_span.innerHTML = 'Linking Task: '+obj.workspace.project[p].linkingTask[l].name;
                    lt_name_a.appendChild(lt_name_span);

                addLeaf(obj.workspace.project[p].linkingTask[l].source,lt_name_li, 'source: ');
                addLeaf(obj.workspace.project[p].linkingTask[l].target,lt_name_li, 'target: ');
                addLeaf(obj.workspace.project[p].linkingTask[l].sourceDataset,lt_name_li, 'source dataset: ');
                addLeaf(obj.workspace.project[p].linkingTask[l].targetDataset,lt_name_li, 'target dataset: ');

                }
        }

        document.getElementById("content").appendChild(tree);

        $("#tree").treeview();
    }