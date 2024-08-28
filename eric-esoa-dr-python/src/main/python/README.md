# eric-bos-dr
This is a python library providing functions for use in D&R feature packs utilizing python scripts.

## In this README

- [Features](#features)
- [Usage](#usage)
    - [Run Rest Service Resource](#run-rest-service-resource)

## Features
The D&R python library provides a client for performing run operations towards Rest Service.

### Rest Service Client
The Rest Service Client provides m

## Usage

### Run Rest Service Resource
The following code snippets demonstrates the use of the 'restservice' module to execute a Rest Service run request.

```
from bos.dr.clients import restservice

payload = payload={"method": "POST", "responseFormat": "json", "inputs": {"input1": "value1"}, "body": {"prop1": "value1"}}
response = restservice.run("target_subsystem_name", "rest_resource_configuration_name", "rest_resource_name", payload)
```