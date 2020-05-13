import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import {
    Card,
    CardContent,
    CardHeader,
    CardTitle,
    Divider,
    TableContainer,
    Table,
    TableHead,
    TableRow,
    TableBody,
    TableCell,
    TableHeader,
    Toolbar,
    ToolbarSection,
    SearchField,
    Button,
    Spacing,
} from "@wrappers/index";
import Loading from "../../../shared/Loading";
import FileUploadModal from "../../../shared/modals/FileUploadModal";
import { EmptyFileWidget } from "./EmptyFileWidget";

export const FileWidget = () => {
    const dispatch = useDispatch();

    const filesList = useSelector(workspaceSel.filesListSelector);
    const fileWidget = useSelector(workspaceSel.widgetsSelector).files;

    const [isOpenDialog, setIsOpenDialog] = useState<boolean>(false);
    const { isLoading } = fileWidget;

    const headers = [
        { key: "name", header: "Name" },
        { key: "type", header: "Type" },
        { key: "formattedDate", header: "Date" },
        { key: "state", header: "State" },
    ];

    useEffect(() => {
        dispatch(workspaceOp.fetchResourcesListAsync());
    }, []);

    const toggleFileUploader = () => {
        setIsOpenDialog(!isOpenDialog);
    };

    return (
        <>
            <Card>
                <CardHeader>
                    <CardTitle>
                        <h3>Files</h3>
                    </CardTitle>
                </CardHeader>
                <Divider />
                <CardContent>
                    {isLoading ? (
                        <Loading />
                    ) : filesList.length ? (
                        <>
                            <Toolbar>
                                <ToolbarSection canGrow>
                                    <SearchField />
                                </ToolbarSection>
                                <ToolbarSection>
                                    <Spacing size="tiny" vertical />
                                    <Button elevated text="Add file" onClick={toggleFileUploader} />
                                </ToolbarSection>
                            </Toolbar>
                            <Spacing size="tiny" />
                            <TableContainer>
                                <Table>
                                    <TableHead>
                                        <TableRow>
                                            {headers.map((property) => (
                                                <TableHeader key={property.key}>{property.header}</TableHeader>
                                            ))}
                                        </TableRow>
                                    </TableHead>
                                    <TableBody>
                                        {filesList.map((file) => (
                                            <TableRow key={file.id}>
                                                {headers.map((property) => (
                                                    <TableCell key={property.key}>{file[property.key]}</TableCell>
                                                ))}
                                            </TableRow>
                                        ))}
                                    </TableBody>
                                </Table>
                            </TableContainer>
                        </>
                    ) : (
                        <EmptyFileWidget onFileAdd={toggleFileUploader} />
                    )}
                </CardContent>
            </Card>
            <FileUploadModal isOpen={isOpenDialog} onDiscard={toggleFileUploader} />
        </>
    );
};
