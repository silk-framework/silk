import React, { Component } from "react";
import { Layout } from "antd";

import Header from "./views/layout/header/Header";
import { connect } from "react-redux";
import { globalOp, globalSel } from "./state/ducks/global";
import RouterOutlet from "./RouterOutlet";
import LanguageContainer from "./LanguageContainer";
import { RouteProps } from "react-router";
import "normalize.css";
import "@blueprintjs/core/lib/css/blueprint.css";
import { ConnectedRouter } from "connected-react-router";
import { getHistory } from "./state/configureStore";

interface IProps {
    routes: RouteProps[];
    externalRoutes: any;
    isAuthenticated: boolean;
    authorize: Function;
}

interface IState {
    loading: boolean;
}

const mapStateToProps = state => ({
    isAuthenticated: globalSel.isAuthSelector(state)
});

const dispatchToProps = dispatch => ({
    authorize: () => dispatch(globalOp.authorize()),
});

class App extends Component<IProps, IState> {
    render() {
        // if (!this.props.isAuthenticated) {
        //     this.props.authorize();
        //     return (
        //         <Layout style={{height: '100vh', justifyContent: 'center'}}>
        //             <Loading />
        //         </Layout>
        //     )
        // }
        return (
            <LanguageContainer>
                <Layout style={{height: '100vh'}}>
                    <ConnectedRouter history={getHistory()}>
                        <Header externalRoutes={this.props.externalRoutes}/>
                        <RouterOutlet routes={this.props.routes}/>
                    </ConnectedRouter>
                </Layout>
            </LanguageContainer>
        );
    }
}

export default connect(mapStateToProps, dispatchToProps)(App);
