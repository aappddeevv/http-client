---
layout: docs
title: Getting Started 
---

# Getting Started

You can chose to use the http and/or the odata layer or use both at the same time.

The library is designed to use combinators to create a client. Some libraries
call this a "backend" but really the "client" concept is the backend. You build
up your client by combining different clients together. For example, to use
odata in the browser, you would select the browser-fetch library, combine it
with the odata client layer.

