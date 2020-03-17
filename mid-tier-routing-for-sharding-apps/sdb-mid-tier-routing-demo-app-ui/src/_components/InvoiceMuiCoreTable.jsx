/*
** Oracle Sharding Tools Library
**
** Copyright © 2019 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

import React from 'react';
import ReactDOM from 'react-dom';
import MuiThemeProvider from 'material-ui/styles/MuiThemeProvider';
import { userService } from '../_services';

import {
  Table,
  TableBody,
  TableHeader,
  TableHeaderColumn,
  TableRow,
  TableRowColumn,
} from 'material-ui/Table';

class InvoiceMuiCoreTable extends React.Component {
    constructor(props){
      super(props);

        this.state = {
          data: [],
          swimLaneInfo : {}
        }
    }

    componentDidMount() {
      console.log('component mount info : ');
      console.log(this.state.swimLaneInfo);

      const user = JSON.parse(localStorage.getItem('user'));
        console.log('user state : ');
        console.log(user);
        console.log('before swim lane call');
        
        console.log(user.swimLaneInfo); 
            
            userService.getUserInvoiceData(user.swimLaneInfo, user.username).then(data => {
                this.setState({ data : data });
                this.data = data;
                console.log(this.data);
              });
    }
    render() {
 

return (   
<MuiThemeProvider>
  <Table>
    <TableHeader>
      <TableRow>
        <TableHeaderColumn>InvoiceId</TableHeaderColumn>
        <TableHeaderColumn>Vendor</TableHeaderColumn>
        <TableHeaderColumn>Balance(USD)</TableHeaderColumn>
        <TableHeaderColumn>Total(USD)</TableHeaderColumn>
        <TableHeaderColumn>Status</TableHeaderColumn>
      </TableRow>
    </TableHeader>
    <TableBody>
      {this.state.data.map(row => {
            return (
                <TableRow>
                <TableRowColumn>{row.INVOICE_ID}</TableRowColumn>
                <TableRowColumn>{row.VENDOR_NAME}</TableRowColumn>
                <TableRowColumn>{row.BALANCE}</TableRowColumn>
                <TableRowColumn>{row.TOTAL}</TableRowColumn>
                <TableRowColumn>{row.STATUS}</TableRowColumn>
                </TableRow>  
            );
      })}
    </TableBody>
    
  </Table>
  </MuiThemeProvider>
 )
}
}


export { InvoiceMuiCoreTable };