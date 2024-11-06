import React, { useContext } from "react";
import {
    ContextOverlay,
    IconButton,
    Label,
    OverflowText,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableRow,
    WhiteSpaceContainer,
} from "@eccenca/gui-elements";
import { SuggestionListContext } from "../../SuggestionContainer";
import { TestableComponent } from "@eccenca/gui-elements/src/components/interfaces";

interface IDataStackItem {
    key: string;
    value: React.ReactNode;
}

interface IDataStack extends TestableComponent {
    data: IDataStackItem[];
}

export function InfoBoxOverlay({ data, ...otherProps }: IDataStack) {
    const { portalContainer } = useContext(SuggestionListContext);
    const dataTestId = (suffix: string) =>
        otherProps["data-test-id"] ? otherProps["data-test-id"] + suffix : undefined;

    return (
        <ContextOverlay
            portalContainer={portalContainer}
            content={
                <WhiteSpaceContainer
                    style={{
                        width: "36rem",
                        maxWidth: "90vw",
                        maxHeight: "40vh",
                        overflow: "auto",
                    }}
                    paddingTop="small"
                    paddingRight="small"
                    paddingBottom="small"
                    paddingLeft="small"
                    data-test-id={dataTestId("-overlay")}
                >
                    <TableContainer>
                        <Table size="small">
                            <colgroup>
                                <col
                                    style={{
                                        width: "4rem",
                                    }}
                                />
                                <col
                                    style={{
                                        width: "20rem",
                                    }}
                                />
                            </colgroup>
                            <TableBody>
                                {data.map((item, idx) =>
                                    item.value ? (
                                        <TableRow
                                            key={item.key ?? idx}
                                            data-test-id={`info-box-row-${item.key ?? idx}`}
                                        >
                                            <TableCell key={"label"}>
                                                <OverflowText passDown={true}>
                                                    <Label text={item.key} isLayoutForElement="span" />
                                                </OverflowText>
                                            </TableCell>
                                            <TableCell key={"description"} data-test-id={"info-box-row-values"}>
                                                {item.value}
                                            </TableCell>
                                        </TableRow>
                                    ) : null
                                )}
                            </TableBody>
                        </Table>
                    </TableContainer>
                </WhiteSpaceContainer>
            }
        >
            <IconButton name="item-info" text="Show more info" data-test-id={dataTestId("-btn")} />
        </ContextOverlay>
    );
}
