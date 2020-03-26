import React from 'react';
import InputGroup from '@wrappers/blueprint/input-group';
import Row from "@wrappers/carbon/grid/Row";
import Col from "@wrappers/carbon/grid/Col";
import {
    Button,
    Card,
    CardHeader,
    CardTitle,
    CardContent,
} from '@wrappers/index';

const PrefixNew = ({onAdd, onChangePrefix, prefix}) => {
    return (
        <Card elevated={0}>
            <CardHeader>
                <CardTitle>
                    <h4>Add Prefix</h4>
                </CardTitle>
            </CardHeader>
            <CardContent>
                <Row>
                    <Col span={7}>
                        <InputGroup
                            value={prefix.prefixName}
                            onChange={(e) => onChangePrefix('prefixName', e.target.value)}
                            placeholder={'Prefix Name'}/>
                    </Col>
                    <Col span={8}>
                        <InputGroup
                            value={prefix.prefixUri}
                            onChange={(e) => onChangePrefix('prefixUri', e.target.value)}
                            placeholder={'Prefix URI'}
                        />
                    </Col>
                    <Col span={1}>
                        <Button
                            onClick={onAdd}
                            elevated
                            disabled={!prefix.prefixName || !prefix.prefixUri}
                        >
                            Add
                        </Button>
                    </Col>
                </Row>
            </CardContent>
        </Card>
    );
};

export default PrefixNew;
