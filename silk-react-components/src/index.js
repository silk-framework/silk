
import React from 'react';
import ReactDOM from 'react-dom';
import superagent from 'superagent';

const Table = ({data, title}) => {
    return (
    <div>
        <strong>{title}</strong>
        <pre>{JSON.stringify(data, null, 2)}</pre>
    </div>
    )
}

class TransformMetadata extends React.Component {

    constructor(props){
        super(props);
        this.state = {
            inputSchemata: {},
            outputSchema: {},
            dependentTasks: [],
        }
        this.getData()
    }

    getData = () => {
        superagent.get(this.props.api)
            .end((err, result) => {
                console.warn(result.body)

                this.setState(
                    result.body
                )
            })
    }

    render() {

        const {inputSchemata, outputSchema, dependentTasks} = this.state;

        return (
            <div>
                <h3>Task Metadata</h3>
                <Table title="Input Schemata" data={inputSchemata} />
                <Table title="Output Schema" data={outputSchema} />
                <Table title="Dependent Tasks" data={dependentTasks} />
                <button onClick={this.getData}>Retrieve Metadata</button>
            </div>
        );
    }
}

const hierarchicalMapping = (containerId, apiUrl) => {
    ReactDOM.render(
      <TransformMetadata api={apiUrl}/>,
      document.getElementById(containerId)
    );
}

window.HierarchicalMapping = hierarchicalMapping;
