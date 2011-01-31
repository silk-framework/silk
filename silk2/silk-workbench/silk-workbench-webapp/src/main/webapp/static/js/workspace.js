function showTree(){

        // TODO read and parse the workspace var
        var obj = jQuery.parseJSON('{"workspace": {   "project": [ {"name": "Project_1", "dataSource":[ {"name":"KEGG","url":"http://kegg.us"},{"name":"Wiki","url":"http://aba.com"}  ],  "linkingTask":[ {"name":"Gene","source":"KEGG","target":"Wiki","targetDataset":"rdf:type Wiki:Gene","sourceDataset":"rdf:type KEGG:gene"},{"name":"linkTask2","source":"KEGG","target":"Wiki"} ] }, {"name": "Project_2", "dataSource":[ {"name":"ABA","url":"http://aba.com"} ] }     ] } }');

        var tree = document.createElement("div");
            tree.id = "div_tree";

        var root = document.createElement("ul");
            root.id = 'tree';
            root.setAttribute('class','filetree');
            tree.appendChild(root);

        for (var p in obj.workspace.project) {
           var  proj = document.createElement("li");
                proj.class= 'closed';
                root.appendChild(proj);
                var proj_span = document.createElement("span");
                    proj_span.setAttribute('class', 'folder');
                    proj_span.innerHTML = obj.workspace.project[p].name;
                    proj.appendChild(proj_span);

            for (var d in obj.workspace.project[p].dataSource){
                var ds_name_ul = document.createElement("ul");
                    proj.appendChild(ds_name_ul);
                var ds_name_li = document.createElement("li");
                    ds_name_ul.appendChild(ds_name_li);
                var ds_name_span = document.createElement("span");
                    ds_name_span.setAttribute('class', 'folder');
                    ds_name_span.innerHTML = 'Data Source: '+obj.workspace.project[p].dataSource[d].name;
                    ds_name_li.appendChild(ds_name_span);

                var ds_url_ul = document.createElement("ul");
                    ds_name_li.appendChild(ds_url_ul);
                var ds_url_li = document.createElement("li");
                    ds_url_ul.appendChild(ds_url_li);
                var ds_url_span = document.createElement("span");
                    ds_url_span.setAttribute('class', 'file');
                    ds_url_span.innerHTML = 'url: '+obj.workspace.project[p].dataSource[d].url;
                    ds_url_li.appendChild(ds_url_span);
            }



            for (var l in obj.workspace.project[p].linkingTask){
                var lt_name_ul = document.createElement("ul");
                    proj.appendChild(lt_name_ul);
                var lt_name_li = document.createElement("li");
                    lt_name_ul.appendChild(lt_name_li);
                var lt_name_a = document.createElement("a");
                    lt_name_a.href = 'linkSpec?'+obj.workspace.project[p].name +':'+obj.workspace.project[p].linkingTask[l].name;
                    lt_name_li.appendChild(lt_name_a);
                var lt_name_span = document.createElement("span");
                    lt_name_span.setAttribute('class', 'folder');
                    lt_name_span.innerHTML = 'Linking Task: '+obj.workspace.project[p].linkingTask[l].name;
                    lt_name_a.appendChild(lt_name_span);

                if (obj.workspace.project[p].linkingTask[l].source){
                var lt_url_ul = document.createElement("ul");
                    lt_name_li.appendChild(lt_url_ul);
                var lt_url_li = document.createElement("li");
                    lt_url_ul.appendChild(lt_url_li);
                var lt_url_span = document.createElement("span");
                    lt_url_span.setAttribute('class', 'file');
                    lt_url_span.innerHTML = 'source: '+obj.workspace.project[p].linkingTask[l].source;
                    lt_url_li.appendChild(lt_url_span);
                }

                if (obj.workspace.project[p].linkingTask[l].target){
                    lt_url_ul = document.createElement("ul");
                    lt_name_li.appendChild(lt_url_ul);
                    lt_url_li = document.createElement("li");
                    lt_url_ul.appendChild(lt_url_li);
                    lt_url_span = document.createElement("span");
                    lt_url_span.setAttribute('class', 'file');
                    lt_url_span.innerHTML = 'target: '+obj.workspace.project[p].linkingTask[l].target;
                    lt_url_li.appendChild(lt_url_span);
                }

                if (obj.workspace.project[p].linkingTask[l].sourceDataset){
                    lt_url_ul = document.createElement("ul");
                    lt_name_li.appendChild(lt_url_ul);
                    lt_url_li = document.createElement("li");
                    lt_url_ul.appendChild(lt_url_li);
                    lt_url_span = document.createElement("span");
                    lt_url_span.setAttribute('class', 'file');
                    lt_url_span.innerHTML = 'source dataset : '+obj.workspace.project[p].linkingTask[l].sourceDataset;
                    lt_url_li.appendChild(lt_url_span);
                  }

                if (obj.workspace.project[p].linkingTask[l].targetDataset){
                    lt_url_ul = document.createElement("ul");
                    lt_name_li.appendChild(lt_url_ul);
                    lt_url_li = document.createElement("li");
                    lt_url_ul.appendChild(lt_url_li);
                    lt_url_span = document.createElement("span");
                    lt_url_span.setAttribute('class', 'file');
                    lt_url_span.innerHTML = 'target dataset : '+obj.workspace.project[p].linkingTask[l].targetDataset;
                    lt_url_li.appendChild(lt_url_span);
                  }

                }
        }

        document.getElementById("content").appendChild(tree);

        $("#tree").treeview();
    }