import React, { useEffect, useState } from "react";
import Card from "../../../../../wrappers/blueprint/card";
import { useDispatch, useSelector } from "react-redux";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import Loading from "../../../../components/Loading";
import { Button, DataTable } from 'carbon-components-react';
import FileUploadModal from "../../../../components/modals/FileUploadModal";

const {
    TableContainer,
    Table,
    TableHead,
    TableRow,
    TableBody,
    TableCell,
    TableHeader,
    TableToolbar,
    TableToolbarSearch,
    TableToolbarContent,
} = DataTable;

const FilesWidget = () => {
    const dispatch = useDispatch();
    const [isOpenDialog, setIsOpenDialog] = useState<boolean>(false);

    const filesList = useSelector(workspaceSel.filesListSelector);
    const fileWidget = useSelector(workspaceSel.widgetsSelector).files;
    const {error, isLoading} = fileWidget;

    const headers = [
        {key: 'name', header: 'Name'},
        {key: 'type', header: 'Type'},
        {key: 'formattedDate', header: 'Date'},
        {key: 'state', header: 'State'},
    ];

    useEffect(() => {
        getFilesList();
    }, []);

    const getFilesList = () => {
        dispatch(workspaceOp.fetchFilesListAsync());
    };

    const handleSearch = (e) => {
        const {value} = e.target;
    };



    const toggleFileUploader = () => {
        setIsOpenDialog(!isOpenDialog);
    };

    const handleFileUpload = (addedFiles) => {

    };

    return (
        <>
            <Card style={{'maxHeight': '250px', 'overflow': 'auto'}}>
                {isLoading ? <Loading/> :
                    <DataTable
                        rows={filesList}
                        headers={headers}
                        render={({rows, headers, getHeaderProps}) => (
                            <TableContainer title="Files">
                                <TableToolbar>
                                    <TableToolbarContent>
                                        <TableToolbarSearch/>
                                        <Button kind={'primary'} onClick={toggleFileUploader}>+ Add File</Button>
                                    </TableToolbarContent>
                                </TableToolbar>
                                <Table>
                                    <TableHead>
                                        <TableRow>
                                            {headers.map(header => (
                                                <TableHeader {...getHeaderProps({header})}>
                                                    {header.header}
                                                </TableHeader>
                                            ))}
                                        </TableRow>
                                    </TableHead>
                                    <TableBody>
                                        {rows.map(row => (
                                            <TableRow key={row.id}>
                                                {row.cells.map(cell => (
                                                    <TableCell key={cell.id}>{cell.value}</TableCell>
                                                ))}
                                            </TableRow>
                                        ))}
                                    </TableBody>
                                </Table>
                            </TableContainer>
                        )}
                    />
                }
            </Card>
            <FileUploadModal
                isOpen={isOpenDialog}
                onDiscard={toggleFileUploader}
                onUpload={handleFileUpload}
                onAbortUploading={() => {}}
                uploadUrl={`/dataintegration/workspace/projects/Full`}
            />
        </>
    )
};

export default FilesWidget;
