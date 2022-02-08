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
} from "gui-elements";
import { SuggestionListContext } from "../../SuggestionContainer";
import {TestableComponent} from "gui-elements/src/components/interfaces";

interface IDataStackItem {
    key: string;
    value: React.ReactNode;
}

interface IDataStack extends TestableComponent {
    data: IDataStackItem[];
}

export function InfoBoxOverlay({data, ...otherProps}: IDataStack) {
    const {portalContainer} =useContext(SuggestionListContext);
    const dataTestId = (suffix: string) => otherProps["data-test-id"] ? otherProps["data-test-id"] + suffix : undefined

    return <ContextOverlay
        portalContainer={portalContainer}
    >
        <IconButton name="item-info" text="Show more info" data-test-id={dataTestId("-btn")} />
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
                <Table size="compact">
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
                        {
                            data.map(item => item.value ? <TableRow key={item.key}>
                                    <TableCell key={"label"}>
                                        <OverflowText passDown={true}>
                                            <Label
                                                text={item.key}
                                                isLayoutForElement="span"
                                            />
                                        </OverflowText>
                                    </TableCell>
                                    <TableCell key={"description"}>
                                        {item.value}
                                    </TableCell>
                                </TableRow> :
                                null
                            )
                        }
                    </TableBody>
                </Table>
            </TableContainer>
        </WhiteSpaceContainer>
    </ContextOverlay>
}
