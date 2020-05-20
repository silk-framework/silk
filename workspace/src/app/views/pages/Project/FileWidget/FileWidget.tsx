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
    Button,
    Spacing,
} from "@wrappers/index";
import Loading from "../../../shared/Loading";
import FileUploadModal from "../../../shared/modals/FileUploadModal";
import { EmptyFileWidget } from "./EmptyFileWidget";
import { SearchBar } from "../../../shared/SearchBar/SearchBar";
import { Highlighter } from "../../../shared/Highlighter/Highlighter";

export const FileWidget = () => {
    const dispatch = useDispatch();

    const filesList = useSelector(workspaceSel.filesListSelector);
    const fileWidget = useSelector(workspaceSel.widgetsSelector).files;
    const [textQuery, setTextQuery] = useState("");

    const [isOpenDialog, setIsOpenDialog] = useState<boolean>(false);
    const { isLoading } = fileWidget;

    const headers = [
        { key: "name", header: "Name", highlighted: true },
        { key: "formattedDate", header: "Date", highlighted: false },
        { key: "formattedSize", header: "Size (bytes)", highlighted: true },
    ];

    const onSearch = (textQuery) => {
        setTextQuery(textQuery);
    };

    useEffect(() => {
        dispatch(workspaceOp.fetchResourcesListAsync({ searchText: textQuery }));
    }, [textQuery]);

    const toggleFileUploader = () => {
        setIsOpenDialog(!isOpenDialog);
    };

    return (
        <>
            <Card>
                <CardHeader>
                    <CardTitle>
                        <h2>Files</h2>
                    </CardTitle>
                </CardHeader>
                <Divider />
                <CardContent>
                    {isLoading ? (
                        <Loading description="Loading file list." />
                    ) : filesList.length ? (
                        <>
                            <Toolbar>
                                <ToolbarSection canGrow>
                                    <SearchBar textQuery={textQuery} onSearch={onSearch} />
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
                                                    <TableCell key={property.key}>
                                                        {property.highlighted ? (
                                                            <Highlighter
                                                                label={file[property.key]}
                                                                searchValue={textQuery}
                                                            />
                                                        ) : (
                                                            file[property.key]
                                                        )}
                                                    </TableCell>
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
