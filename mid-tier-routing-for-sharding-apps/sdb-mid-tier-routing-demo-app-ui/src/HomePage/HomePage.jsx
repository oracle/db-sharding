/*
** Oracle Sharding Tools Library
**
** Copyright © 2019 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

import React from 'react';
import { Link } from 'react-router-dom';
import { InvoiceMuiCoreTable } from '../_components';

class HomePage extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            user: {},
            users: [],
            swimLaneInfo: {}
        };
    }

    componentDidMount() {
        this.setState({
            user: JSON.parse(localStorage.getItem('user')),
            users: { loading: true }
        });

        this.state.user = JSON.parse(localStorage.getItem('user'));
        console.log('user state : ');
        console.log(this.state.user);
        console.log('before swim lane call');
    }

    render() {
        const { user, users } = this.state;

        return (
            <div>
                <center>
                    <h2>Welcome back {user.firstName} !</h2>
                </center>
                <br />
                <br />
                <h3>Vendor Invoices</h3>
                <InvoiceMuiCoreTable />
                <br />
                <br />
                <center><Link to="/login">Logout</Link></center>
            </div>
        );
    }
}

export { HomePage };