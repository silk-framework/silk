import React from "react";
import { Layout } from "antd";
import { useDispatch } from "react-redux";
import { globalOp } from "../../../state/ducks/global";

export default function DashboardLayout() {
    const {Content} = Layout;
    const dispatch = useDispatch();

    return (
        <Layout>
            <Content style={{padding: "20px 50px"}}>
                <button onClick={() => dispatch(globalOp.logout())}>Auth false</button>
                <button onClick={() => dispatch({type: '__dev__/RESET_STORE'})}>Reset store to default</button>
            </Content>
        </Layout>
    )
}
