/*
** Oracle Sharding Tools Library
**
** Copyright © 2019 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

import React from 'react';
import { render } from 'react-dom';
import "@babel/polyfill";

import { App } from './App';

// setup fake backend
import { configureBackend } from './_helpers';
configureBackend();

render(
    <App />,
    document.getElementById('app')
);