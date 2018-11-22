---
layout: docs
title: odata-client 
---

# Getting Started

Usage varies depending on browser vs server usage and on how you integrated security protocols such as JWT.

# Browser
If you are using the client browser side, you need to create a client with the types of middleware you wish to use. Middleware can handle everything from retries to security.


# Server

```sh
# file dynamics.json exist in the current working directory
# no need for -c dynamics.json
dynamicscli command subcommand args optional-args
```

If the environment variable `DYNAMCIS_CRMCONFIG` is defined, that value will be
used as the name of the connection file. The precedence is the CLI parameter,
the environment variable and then the default `./dynamics.json`.

Environment variables include:

* DYNAMICS_CRMCONFIG: Connection config file
* DYNAMICS_IMPERSONATE: Impersonate crm systemuserid. You must use the dynamics GUID. You can obtain the guid via `dynamicscli users list`.
* DYNAMICS_PASSWORD: dynamics org password. You can also include this in the config file.

Throughout the rest of the documentation, we will assume that DYNAMICS_CRMCONFIG
is pointing to a config file and leave the argument (--c or --crm-config) off of
the examples.
