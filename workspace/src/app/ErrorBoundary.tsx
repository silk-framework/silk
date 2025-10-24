import React, { Component } from "react";
import { logError } from "./services/errorLogger";
import { isDevelopment, PUBLIC_URL, SERVE_PATH } from "./constants/path";
import {
    Button,
    Grid,
    GridColumn,
    GridRow,
    HtmlContentBlock,
    Icon,
    Notification,
    TitleMainsection,
} from "@eccenca/gui-elements";

/**
 * Catch the children components errors
 * @see https://reactjs.org/blog/2017/07/26/error-handling-in-react-16.html
 * @see https://github.com/facebook/react/issues/11334#issuecomment-338656383
 */
class ErrorBoundary extends Component<any, any> {
    constructor(props) {
        super(props);
        this.state = { hasError: false };
    }

    componentDidCatch(error, info) {
        logError(error, info);
        if (isDevelopment) {
            console.log(error, info);
        }
    }

    static getDerivedStateFromError() {
        return { hasError: true };
    }

    render() {
        if (this.state.hasError) {
            return (
                <Grid>
                    <GridRow fullHeight>
                        <div style={{ margin: "auto", maxWidth: "40rem" }}>
                            <GridColumn verticalAlign={"center"}>
                                <Notification
                                    intent="danger"
                                    actions={
                                        <>
                                            <Button
                                                minimal
                                                outlined
                                                icon={<Icon name="application-homepage" />}
                                                text="Go to homepage"
                                                href={PUBLIC_URL + SERVE_PATH}
                                            />
                                            <Button
                                                minimal
                                                outlined
                                                text="Reload page"
                                                onClick={() => window.location.reload()}
                                            />
                                        </>
                                    }
                                >
                                    <HtmlContentBlock>
                                        <TitleMainsection>Unexpected error</TitleMainsection>
                                        <p>If you think something went wrong then please inform your administrator.</p>
                                    </HtmlContentBlock>
                                </Notification>
                            </GridColumn>
                        </div>
                    </GridRow>
                </Grid>
            );
        }

        return this.props.children;
    }
}

export default ErrorBoundary;
