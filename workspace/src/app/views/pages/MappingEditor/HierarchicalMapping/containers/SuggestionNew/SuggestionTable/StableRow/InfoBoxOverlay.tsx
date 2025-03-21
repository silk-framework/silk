import React, { useContext } from "react";
import {
    ContextOverlay,
    IconButton,
    Label,
    OverflowText,
    PropertyName,
    PropertyValue,
    PropertyValueList,
    PropertyValuePair,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableRow,
    WhiteSpaceContainer,
    WhiteSpaceContainerProps,
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

export interface InfoBoxOverlayDisplayProps {
    /** If true the content will be returned directly instead being shown as context overlay. */
    embed?: boolean;
    /** how it is displayed */
    dislayType?: "table" | "propertylist";
    /** forwading or overwriting container properties */
    whiteSpaceContainerProps?: Omit<WhiteSpaceContainerProps, "children">;
}

export function InfoBoxOverlay({
    data,
    embed = false,
    dislayType = "table",
    whiteSpaceContainerProps,
    ...otherProps
}: IDataStack & InfoBoxOverlayDisplayProps) {
    const { portalContainer } = useContext(SuggestionListContext);
    const dataTestId = (suffix: string) =>
        otherProps["data-test-id"] ? otherProps["data-test-id"] + suffix : undefined;
    const Content = () => {
        return (
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
                {...whiteSpaceContainerProps}
                data-test-id={dataTestId("-overlay")}
            >
                {dislayType === "table" ? (
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
                ) : (
                    <PropertyValueList singleColumn>
                        {data.map((item, idx) =>
                            item.value ? (
                                <PropertyValuePair
                                    key={item.key ?? idx}
                                    data-test-id={`info-box-row-${item.key ?? idx}`}
                                >
                                    <PropertyName nowrap>{item.key}</PropertyName>
                                    <PropertyValue>{item.value}</PropertyValue>
                                </PropertyValuePair>
                            ) : null
                        )}
                    </PropertyValueList>
                )}
            </WhiteSpaceContainer>
        );
    };

    if (data.length === 0) {
        return null;
    }

    return embed ? (
        <Content />
    ) : (
        <ContextOverlay portalContainer={portalContainer} content={<Content />}>
            <IconButton name="item-info" text="Show more info" data-test-id={dataTestId("-btn")} />
        </ContextOverlay>
    );
}
