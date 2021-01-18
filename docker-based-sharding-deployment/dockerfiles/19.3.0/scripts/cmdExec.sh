#!/bin/bash

echo $@ > /tmp/command.txt

cmd=$@

$cmd

