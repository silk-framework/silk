import React from 'react';
import chai, {expect} from 'chai';
import chaiEnzyme from 'chai-enzyme'
import Enzyme, { mount, shallow } from 'enzyme';
import Adapter from 'enzyme-adapter-react-15';
import nock from 'nock';
import HierarchicalMapping from '../src/HierarchicalMapping/HierarchicalMapping';
import SilkStore from '../src/SilkStore/silkStore'; // Required for the store to work.
import './server/HierarchicalMapping_server';


describe('HierarchicalMapping', () => {

    const mountedComponent = mount(
        <HierarchicalMapping
            baseUrl={"http://localhost:8080"}
            project={"cmem"}
            transformTask={"transform_datasetresource_Sacramentorealestatetransactions_csv"}
            inititalRule={"root"}
        />
    );


    it('the component contains a main view with 3 items', () => {

        setTimeout(() => {
            const component = mountedComponent.render();

            const mainView = component.find(".ecc-silk-mapping__rules");
            expect(mainView).to.have.length(1);
            const items = component.find('.ecc-silk-mapping__ruleitem-summary');
            expect(items).to.have.length(3);

        }, 5000)
    });

});