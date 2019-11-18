import React from "react";
import { Layout } from "antd";
import { Button } from "@blueprintjs/core";

import { useDispatch } from "react-redux";
import { globalOp } from "../../../state/ducks/global";

export default function DashboardLayout() {
    const {Content} = Layout;
    const dispatch = useDispatch();

    return (
        <Layout>
            <Content style={{padding: "20px 50px"}}>
                <Button intent={'success'} text="Auth false" onClick={() => dispatch(globalOp.logout())}/>
                <Button intent={'success'} text="Reset store to default" onClick={() => dispatch({type: '__dev__/RESET_STORE'})} />
            </Content>
        </Layout>
    )
}
