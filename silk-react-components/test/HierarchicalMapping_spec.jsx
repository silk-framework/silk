import React from 'react';
import chai, {expect} from 'chai';
import chaiEnzyme from 'chai-enzyme'
import Enzyme, { mount, shallow, ReactWrapper } from 'enzyme';
import Adapter from 'enzyme-adapter-react-15';
import nock from 'nock';
import HierarchicalMapping from '../src/HierarchicalMapping/HierarchicalMapping';
import SilkStore from '../src/SilkStore/silkStore'; // Required for the store to work.
import './server/HierarchicalMapping_server';

const component = mount(
    <HierarchicalMapping
        baseUrl={"http://docker.local/dataintegration"}
        project={"cmem"}
        transformTask={"transform_datasetresource_1537869610185_orgmap_xml"}
        inititalRule={"root"}
    />
);

function sleep(ms) {
    return new Promise(resolve => {
        setTimeout(resolve, ms)
    });
}

describe('HierarchicalMapping', () => {

    it('a form edit rule is accessible', async () => {
        await sleep(100);
        component.update()
        const button = component.find('button.silkobject')
        expect(button).to.have.lengthOf(1);
        button.simulate("click")
        await sleep(100)
        component.update()
        const button2 = component.find('button.silkobject')
        expect(button2).to.have.lengthOf(1);
        button2.simulate("click")
        await sleep(100)
        component.update()
        const button3 = component.find('button.ecc-silk-mapping__rulesviewer__actionrow-edit')
        expect(button3).to.have.lengthOf(1);
        button3.simulate("click")
        await sleep(100)
        component.update()
        const field = component.find('.ecc-silk-mapping__ruleseditor__sourcePath .Select--single')
        expect(field).to.have.lengthOf(1);
        field.simulate('click')
        await sleep(100)
        component.update()
        const options = component.find('.ecc-silk-mapping__ruleseditor__sourcePath option')
        //expect(options).to.have.lengthOf(26);
        // The component show only options when the element is focused,
        // so we cant really test which options are rendered.

    });


});