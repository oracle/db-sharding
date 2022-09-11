/*
** Oracle Sharding Tools Library
**
** Copyright © 2019 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

import React from 'react';
import { Tabs } from 'react-bootstrap';
import { Tab } from 'react-bootstrap';
import { LoginPage } from '../LoginPage';

class LoginWrapper extends React.Component {
    constructor(props, context) {
      super(props, context);
      this.state = {
        key: 'individual',
      };
    }
  
    render() {
      return (
        <Tabs
          id="controlled-tab-login-wrapper"
          activeKey={this.state.key}
          onSelect={key => this.setState({ key })}
        >
          <Tab eventKey="individual" title="Individual">
            <LoginPage />
          </Tab>
          <Tab eventKey="business" title="Business">
            <LoginPage />
          </Tab>
        </Tabs>
      );
    }
  }
  
  export { LoginWrapper };