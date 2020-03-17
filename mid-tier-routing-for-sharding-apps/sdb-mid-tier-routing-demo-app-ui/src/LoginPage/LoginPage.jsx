/*
** Oracle Sharding Tools Library
**
** Copyright © 2019 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/


import React from 'react';

import { userService } from '../_services';

class LoginPage extends React.Component {
    constructor(props) {
        super(props);

        userService.logout();

        this.state = {
            username: '',
            password: '',
            submitted: false,
            loading: false,
            error: '',
            custType: ''
        };

        this.handleChange = this.handleChange.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
    }

    handleChange(e) {
        const { name, value } = e.target;
        this.setState({ [name]: value });
    }

    handleSubmit(e) {
        e.preventDefault();

        userService.logout();

        this.setState({ submitted: true });
        const { username, password, custType} = this.state;

        // stop here if form is invalid
        if (!(username && password && custType)) {
            return;
        }

        this.setState({ loading: true });
        userService.userLogin(username, password, custType)
            .then(
                user => {
                    const { from } = this.props.location.state || { from: { pathname: "/" } };
                    this.props.history.push(from);
                },
                error => this.setState({ error, loading: false })
            );
    }

    render() {
        const { username, password, submitted, loading, error } = this.state;
        return (
            <div className="col-md-6 col-md-offset-3">
                <center>
                    <h2>Invoice App</h2>
                </center>
                <br />
                <br />
                <form name="form" onSubmit={this.handleSubmit}>
                    <div className={'form-group' + (submitted && !username ? ' has-error' : '')}>
                        <label htmlFor="username">Customer ID</label>
                        <input type="text" className="form-control" name="username" value={username} onChange={this.handleChange} />
                        {submitted && !username &&
                            <div className="help-block">Username is required</div>
                        }
                    </div>
                    <div className={'form-group' + (submitted && !password ? ' has-error' : '')}>
                        <label htmlFor="password">Password</label>
                        <input type="password" className="form-control" name="password" value={password} onChange={this.handleChange} />
                        {submitted && !password &&
                            <div className="help-block">Password is required</div>
                        }
                    </div>
                    <div className="form-group">
                        <label>
                            <input
                                type="radio"
                                name="custType"
                                value="individual"
                                checked={this.state.custType === "individual"}
                                onChange={this.handleChange}
                            />
                            Individual
                        </label>
                        &nbsp;
                        &nbsp;
                        &nbsp;
                        &nbsp;

                        <label>
                            <input
                                type="radio"
                                name="custType"
                                value="business"
                                checked={this.state.custType === "business"}
                                onChange={this.handleChange}
                            />
                           Business
                        </label>
                    </div>
                        <div className="form-group">
                            <button className="btn btn-primary" disabled={loading}>Login</button>
                            {loading}
                        </div>
                        {error &&
                            <div className={'alert alert-danger'}>{error}</div>
                        }
                </form>
            </div>
                );
            }
        }
        
export {LoginPage}; 