# http-client

A simple simple scalajs client for native/js/jvm environments such as browsers
and servers. It includes both an http and odata layer layer. The http layer can
be used independently of the odata layer.

What's unique in this client library? 

* The lower level client/backend details are exposed in order to allow you to
optimize the use of the underlying transport API. Many other client libraries
try to harmonize and hide the back-end details. However, hiding the back-end
leads to sub-optimal memory and compute resource usage. This library exposes
those details in a type safe way so that if you *want* to better control
resources, you can.
* Since it is not dependent on any other library, it has a relatively small
footprint--important on the browser. Individual specific client implementations
may have other dependencies but the core library is very small and potentially
allows you having bundle in the scala collections library.
* The steaming API is separate from the core
HTTP API because streaming target types are highly varied and can arise from
both HTTP-level or server-side (think paging) protocols.

The initial focus is on javascript environments using scala.js.

Because a clients/backends specific types *could* be exposed, the basic classes
take more type parameters. The burden of using these types is hidden using smart
constructors, etc.

The basic functional type flow for a request/response is:

* Domain object A to wrapped client/backend type C1: B1[A] => B1[C1]
* HTTP request/response: B1[C1] => B2[C2]
* Domain object extract to wrapped T: B2[C2] => F[T]

While you *could* use the async effect everywhere, you do not need to and it is
sometimes often advantageous to not use client/backend specific data types.

NOTE: EXTRACTION AND REFACTORING IS IN PROGRESS. STAY TUNED!

# Fast Start

```scala
// use cats-effect IO
implicit val client = BrowserFetchClient[IO]()
val myRequest = http.empty.path("/accounts")
implicit val decoder = decoders.fastJson

// Client API centric, no implicits needed
client.fetchAs(myRequest)(decoder)
  .
  
// Client API centric, no implicit needed for decoder
clent.fetchAs(myRequest)
  .

// Request object API centric 
myRequest.send
  .

```


# Other HTTP clients

There are already a few HTTP clients:

* [shttp](https://sttp.readthedocs.io/en/latest/index.html)
* [RosHTTP](https://github.com/hmil/RosHTTP)
* [scalaj-http](https://github.com/scalaj/scalaj-http)
* [http4s](https://github.com/http4s)
* Many more!

And more, such as akka http. However not all clients are available under
scala.js.


# License

MIT license.

Copyright 2019. The Trapelo Group, LLC.
