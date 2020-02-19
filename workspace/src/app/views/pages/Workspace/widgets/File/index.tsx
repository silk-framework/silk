import React, { useEffect } from "react";
import Card from "../../../../../wrappers/card";
import { useDispatch, useSelector } from "react-redux";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import InputGroup from "@wrappers/input-group";
import Loading from "../../../../components/Loading";

const FilesWidget = () => {
    const dispatch = useDispatch();
    const filesList = useSelector(workspaceSel.filesListSelector);
    const fileWidget = useSelector(workspaceSel.widgetsSelector).files;
    const {error, isLoading} = fileWidget;

    useEffect(() => {
        getFilesList();
    }, []);

    const getFilesList = () => {
        dispatch(workspaceOp.fetchFilesListAsync());
    };

    const handleSearch = (e) => {
        const {value} = e.target;
    };

    return (
        <Card style={{'max-height': '250px', 'overflow': 'auto'}}>
            <h3>Files</h3>
            <div>
                {
                    isLoading ? <Loading/> :
                        !filesList.length
                            ? 'No files found, add them here to use it later'
                            : <>
                                <table>
                                    <thead>
                                        <tr><InputGroup onChange={handleSearch}/></tr>
                                        <tr>
                                            <th>Name</th>
                                            <th>Type</th>
                                            <th>Date</th>
                                            <th>State</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                    {
                                        filesList.map(file =>
                                            <tr key={file.name}>
                                                <td>{file.name}</td>
                                                <td>-</td>
                                                <td>{file.modified}</td>
                                                <td>-</td>
                                            </tr>
                                        )
                                    }
                                    </tbody>
                                </table>
                            </>
                }
            </div>
        </Card>
    )
};

export default FilesWidget;
