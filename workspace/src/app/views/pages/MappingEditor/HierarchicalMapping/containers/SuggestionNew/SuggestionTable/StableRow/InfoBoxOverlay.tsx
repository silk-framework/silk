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
import {TestableComponent} from "@eccenca/gui-elements/src/components/interfaces";

interface IDataStackItem {
    key: string;
    value: React.ReactNode;
}

interface IDataStack extends TestableComponent {
    data: IDataStackItem[];
    /** If true the content will be returned directly instead being shown as context overlay. */
    embed?: boolean
}

export function InfoBoxOverlay({data, embed = false,...otherProps}: IDataStack) {
    const {portalContainer} =useContext(SuggestionListContext);
    const dataTestId = (suffix: string) => otherProps["data-test-id"] ? otherProps["data-test-id"] + suffix : undefined
    const Content = () => <WhiteSpaceContainer
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
                        data.map((item, idx) => item.value ? <TableRow key={item.key ?? idx} data-test-id={`info-box-row-${item.key ?? idx}`}>
                                    <TableCell key={"label"}>
                                        <OverflowText passDown={true}>
                                            <Label
                                                text={item.key}
                                                isLayoutForElement="span"
                                            />
                                        </OverflowText>
                                    </TableCell>
                                    <TableCell key={"description"} data-test-id={"info-box-row-values"}>
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

    return embed?
        <Content/> :
        <ContextOverlay
            portalContainer={portalContainer}
            content={<Content/>}
        >
            <IconButton name="item-info" text="Show more info" data-test-id={dataTestId("-btn")}/>
        </ContextOverlay>
}
