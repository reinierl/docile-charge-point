# docile-charge-point

A scriptable [OCPP](http://openchargealliance.org/protocols/ocpp/ocpp-16/) charge point simulator.

Not as continuously ill-tempered as
[abusive-charge-point](https://github.com/chargegrid/abusive-charge-point), but
it can be mean if you script it to be.

The aims for this thing:

 * Simulated charge point behaviors expressed as simple scripts, that can be redistributed separately from the simulator that executes them

 * Simulate lots of charge points at once that follow given behavior scripts

 * Checkable test assertions in the scripts

 * Non-interactive command line interface, which combined with the test assertions makes it useful for use in CI/CD pipelines

Scripts are expressed as Scala files, in which you can use predefined functions
to send OCPP messages, make expectations about incoming messages or declare the
test case failed. And next to that, all of Scala is at your disposal! Examples
of behavior scripts it can run already are [a simple heartbeat script](examples/heartbeat.scala) and
[a full simulation of a charge session](examples/do-a-transaction.scala). The full set of OCPP and testing specific
functions can be found in
[CoreOps](src/main/scala/chargepoint/docile/dsl/CoreOps.scala),
[expectations.Ops](src/main/scala/chargepoint/docile/dsl/expectations/Ops.scala)
and [shortsend.Ops](src/main/scala/chargepoint/docile/dsl/shortsend/Ops.scala).

You can run the simulator like this, from the root directory of the project:

```
sbt 'run -c <charge point ID> -v <OCPP version> <Central System endpoint URL> <test scripts...>'
```

so e.g.:

```
sbt 'run -c chargepoint0123 -v 1.6 ws://example.org/ocpp-j-endpoint examples/heartbeat.scala'
```

See `sbt 'run --help'` for more options.

## Script structure

The charge point scripts that you specify on the command line are ordinary
Scala files, that will be loaded and executed at runtime by
docile-charge-point. To write these files, besides the normal standard Scala
library, you can use a DSL in which you can send OCPP messages and expect a
certain behavior in return from the Central System.

### Simple one-line scripts

As a very simple example of the DSL, consider this script:

```scala
heartbeat()
```

Simple as it looks, this script already does two things:

 * It sends an OCPP heartbeat request
 * It asserts that the Central System responds with an OCPP heartbeat response

In fact, there are such functions doing these two things for every request in OCPP 1.6 that a Charge Point can send to a Central System. There is a `statusNotification` that will, indeed, send a StatusNotification request.

Where these requests contain fields with data, these data can be given by supplying values for certain named arguments of those functions. By default, `statusNotification()` will mark the charge point as _Available_. To make it seem _Charging_ instead, do this:

```
statusNotification(status = ChargePointStatus.Occupied(Some(OccupancyKind.Charging)))
```

Ouch, that's quite some code to express the OCPP 1.6 charging state. Let me
digress to explain it. The reason for it is that this tool uses the case class
hierarchy from the [NewMotion OCPP library](https://github.com/NewMotion/ocpp/)
to express OCPP messages and their constituents parts. Those case classes
however are different from the data types found in the OCPP specification, to
encode the information in a more type-safe manner and to provide an abstract
representation that can be transmitted in either a 1.6 or a 1.5 format. Here it
is the compatibility with 1.5 that means that we can't just write
`ChargePointStatus.Charging`, because that couldn't be serialized for 1.5.
`ChargePointStatus.Occupied(Some(OccupancyKind.Charging))` means: _Occupied_ for
1.5 terms, but if you want to encode it for 1.6, you can be more precise and
make it _Charging_.

This kind of abstraction from version-specific messages can be a useful feature
in some scenarios: you can now easily test that a back-office handles a certain
behavior correctly both with 1.5 and 1.6.

An interactive session with tab completion (see below) can come in handy to
explore the ways you can specify OCPP messages.

### Stringing operations together

As an example of how you can string DSL operations together to specify a
meaningful behavior, let's look at the "do a transaction" example in its full
glory. There are comments explaining what happens where:

```scala
// The idTag which can be used to look up who started the transaction
// This is ordinary Scala defining a string value; no docile DSL so far.
val chargeTokenId = "01234567"

// Now let's send an authorize request to the Central System and expect a
// response. If the expected response comes, it is returned from the
// `authorize` function.
// This response objet then contains an `idTag` field with the authorization
// information from the Central System. We call that authorization info `auth`.
val auth = authorize(chargeTokenId).idTag

// We check whether the charge token we sent is authorized. This is just plain
// Scala again.
if (auth.status == AuthorizationStatus.Accepted) {

  // If it's authorized, we start a transaction. Starting a transaction in OCPP
  // means: first set the status to Preparing...
  statusNotification(status = ChargePointStatus.Occupied(Some(OccupancyKind.Preparing)))

  // ...then start a transaction. The startTransaction function again returns
  // the StartTransaction response from the Central System, from which we take
  // the transaction ID and assign it the name `transId`
  val transId = startTransaction(meterStart = 300, idTag = chargeTokenId).transactionId
  
  // ... and then, we notify the Central System that this charge point has
  // started charging.
  statusNotification(status = ChargePointStatus.Occupied(Some(OccupancyKind.Charging)))

  // Another DSL operation: prompt. This allows us to prompt the user for some
  // keyboard input. Here, we just want him to press ENTER when
  // docile-charge-point should stop the transaction.
  // This `prompt` function will block until the user presses ENTER.
  prompt("Press ENTER to stop charging")

  // Okay, the user has apparently pressed ENTER. Let's stop.
  // First notify that the status of our connector is going to Finishing...
  statusNotification(status = ChargePointStatus.Occupied(Some(OccupancyKind.Finishing)))

  // ...then send a StopTransaction request...
  stopTransaction(transactionId = transId, idTag = Some(chargeTokenId))

  // ...and notify that our connector is Available again
  statusNotification(status = ChargePointStatus.Available())

// Oh yeah, it's also possible that the Central System does not authorize the
// transaction
} else {
  // In that case we consider this script failed.
  fail("Not authorized")
}
```

The important take-aways here are:

  * DSL operations are just Scala function calls

  * Typically, DSL operations will block until the user input or Central System
    response has come, and will return this result as from the function call

### Expectations

The semi-final line is interesting: `fail("Not authorized")`.

This shows that docile-charge-point scripts don't just run. They run, and in the
end docile-charge-point will consider them either _failed_ or _passed_. Also, if
a script inadvertently fails to run at all, docile-charge-point will consider
the outcome an _error_.

If I for instance run both the example heartbeat script and the example
do-a-transaction script, against a back-office that does not authorize the
transaction, I will see that one script failed and the other one passed. In the
console, that looks like this:

```
sbt 'run -c '03000001' ws://test-chargenetwork.thenewmotion.com/ocppws examples/heartbeat.scala examples/do-a-transaction.scala'
Loading settings from plugins.sbt ...
Loading project definition from /Users/reinier/Documents/Programs/docile-charge-point/project
Loading settings from build.sbt ...
Set current project to docile-charge-point (in build file:/Users/reinier/Documents/Programs/docile-charge-point/)
Credentials file /Users/reinier/.ivy2/.credentials does not exist
Packaging /Users/reinier/Documents/Programs/docile-charge-point/target/scala-2.11/docile-charge-point_2.11-0.1-SNAPSHOT.jar ...
Done packaging.
Running (fork) chargepoint.docile.Main -c 03000001 ws://test-chargenetwork.thenewmotion.com/ocppws examples/heartbeat.scala examples/do-a-transaction.scala
Going to run heartbeat
>> HeartbeatReq
<< HeartbeatRes(2018-04-02T20:38:13.342Z[UTC])
Going to run do-a-transaction
>> AuthorizeReq(01234567)
<< AuthorizeRes(IdTagInfo(IdTagInvalid,None,Some(01234567)))
heartbeat: ✅
do-a-transaction: ❌  Not authorized
```

So docile-charge-point will show that the heartbeat script passed, and the
do-a-transaction script failed with the message "Not authorized".

Also, the command will return success (exit status 0) if all scripts passed, and
failure (exit status 1) otherwise.

It is now also time to come back to the statement about `statusNotification()`,
saying that this simple call did two things. In fact, this function will send
the message, and then wait for the response, and make the script fail if the
first incoming message is not a StatusNotification response. This is usually
useful in order to get a response object to work with, but sometimes you'd
want your script to be more flexible about how the Central System can respond.

For those cases you have the `send` and `expectIncoming` functions in the DSL.
`send` sends a message to the Central System, and immediately returns without
waiting for a response. `expectIncoming` in turn looks if a message has been
received from the Central System, and if not, will block until one arrives.

The `statusNotification()` call turns out to be equivalent to:

```
send(StatusNotificationReq(
  scope = ConnectorScope(0),
  status = ChargePointStatus.Available(),
  timestamp = Some(ZonedDateTime.now()),
  vendorId = None
))
expectIncoming(matching { case res@StatusNotificationRes => res })
```

So this `expectIncoming(matching ...)` line is in the end also an expression that
returns the response that was just received.

What `expectIncoming` does comes down to:

 * Get the first incoming message that has not been expected before by the script, waiting for it if there is no such incoming message yet

 * See if this message matches the partial function that's given after
   `matching`

 * If so, return the result of the partial function. If not, fail the script.


In order to feed `expectIncoming`, docile-charge-point keeps a queue of messages
that have been received. The `expectIncoming` call is always evaluated against
the head of the queue. So you _have_ to expect every message the Central System
sends to you, in the order in which they arrive!

To make this order requirement easier to deal with, you can also expect
multiple messages at once, and docile-charge-point will accept them no matter
in which order they arrive:

```
expectInAnyOrder(
  remoteStartTransactionReq.respondingWith(RemoteStartTransactionRes(true)),
  changeConfigurationReq.respondingWith(ChangeConfigurationRes(ConfigurationStatus.Accepted))
)
```

As a variant of the `expectIncoming(matching ...)` idiom, there is also an
`expectIncoming requestMatching ...` variant, that lets you expect incoming
requests from the Central System, and respond to them, like so:

```
      expectIncoming(
        requestMatching({case r: RemoteStopTransactionReq => r.transactionId == transId})
          .respondingWith(RemoteStopTransactionRes(_))
      )
```

This bit waits for an incoming StopTransaction request, and fails if the next
incoming message is not a StopTransaction request. If it is, it returns whether
the transaction ID in that message matches the `transId` value. Also, it
responds to the Central System with a RemoteStopTransaction response.

The argument to `respondingWith` can either be a literal value, or it can be a
function from the result of the partial function to a response. Here the latter
option is used in order to tell the Central System whether the remote stop
request is accepted, based on whether the remote stop request's transaction ID
matched the one that the script had started.

See [the remote start/stop example](examples/remote-transaction.scala) for the
full script using all these features.

As you can see in the handling of the remote start request there, there is also
a shorthand for expecting an incoming request of a certain type, without caring
more about the specific message contents. So this bit:

```
expectIncoming(remoteStartTransactionReq.respondingWith(RemoteStartTransactionRes(true)))
```

is equivalent to:

```
expectIncoming(
  requestMatching({case r: StartTransactionReq => r})
    .respondingWith(RemoteStartTransactionRes(true))
)
```

## Interactive use

You can also go into an interactive testing session on the command line.

To get an interactive terminal, it's easiest to first compile using sbt:

```
sbt assembly
```

And then run `docile-charge-point` directly using the `java` command:

```
java -jar target/scala-2.11/docile.jar -i -v 1.6 -c chargepoint0123 ws://example.com/ocpp
```

The `-i` option here tells `docile-charge-point` to go into interactive mode.

The app will start and something write this to the console:

```
[info, chargepoint.docile.test.InteractiveRunner] Going to run Interactive test
Compiling (synthetic)/ammonite/predef/interpBridge.sc
Compiling (synthetic)/ammonite/predef/replBridge.sc
Compiling (synthetic)/ammonite/predef/DefaultPredef.sc
Compiling (synthetic)/ammonite/predef/ArgsPredef.sc
Compiling (synthetic)/ammonite/predef/CodePredef.sc
Welcome to the Ammonite Repl 1.0.3
(Scala 2.11.11 Java 1.8.0_144)
If you like Ammonite, please support our development at www.patreon.com/lihaoyi
@
```

The `@` sign on that last line is your prompt. You can now type expressions in the `docile-charge-point` DSL, like:

```
statusNotification()
```

and you'll see the docile-charge-point and the back-office exchange messages:

```
[info, chargepoint.docile.test.InteractiveOcppTest$$anon$1] >> StatusNotificationReq(ConnectorScope(0),Occupied(Some(Charging),None),Some(2018-01-01T15:12:43.251+01:00[Europe/Paris]),None)
[info, chargepoint.docile.test.InteractiveOcppTest$$anon$1] << StatusNotificationRes
```

let's see what happens if we send a timestamp from before the epoch...

```
statusNotification(timestamp = Some(ZonedDateTime.of(1959, 1, 1, 12, 0, 0, 0, ZoneId.of("Z"))))
```

turns out it works surprisingly well :-):

```
[info, chargepoint.docile.test.InteractiveOcppTest$$anon$1] >> StatusNotificationReq(ConnectorScope(0),Available(None),Some(1959-01-01T12:00Z),None)
[info, chargepoint.docile.test.InteractiveOcppTest$$anon$1] << StatusNotificationRes
```

You'll also see that the interactive mode prints something like this:

```
res0: StatusNotificationRes.type = StatusNotificationRes
```

That's the return value of the expression you entered, which in this case, is the StatusNotification response object. And because you're in a full-fledged Scala REPL using [Ammonite](ammonite.io), nothing is stopping you from doing fancy stuff with that. So you can for instance values from responses in subsequent requests:

```
@ startTransaction(idTag = "ABCDEF01")
[info, chargepoint.docile.test.InteractiveOcppTest$$anon$1] >> StartTransactionReq(ConnectorScope(0),ABCDEF01,2018-01-01T15:22:30.122+01:00[Europe/Paris],0,None)
[info, chargepoint.docile.test.InteractiveOcppTest$$anon$1] << StartTransactionRes(177,IdTagInfo(Accepted,None,Some(ABCDEF01)))
res3: StartTransactionRes = StartTransactionRes(177, IdTagInfo(Accepted, None, Some("ABCDEF01")))

@ stopTransaction(transactionId = res3.tr
transactionId
@ stopTransaction(transactionId = res3.transactionId)
[info, chargepoint.docile.test.InteractiveOcppTest$$anon$1] >> StopTransactionReq(177,Some(ABCDEF01),2018-01-01T15:22:50.457+01:00[Europe/Paris],16000,Local,List())
[info, chargepoint.docile.test.InteractiveOcppTest$$anon$1] << StopTransactionRes(Some(IdTagInfo(Accepted,None,Some(ABCDEF01))))
res4: StopTransactionRes = StopTransactionRes(Some(IdTagInfo(Accepted, None, Some("ABCDEF01"))))
```

Note also that between those two requests, I used tab completion to look up the name of the `transactionId` field in the StopTransaction request.

## TODOs

It's far from finished now. The next steps I plan to develop:

 * More robust handling of connection closes and OCPP errors

 * Make it able to take both Central System and Charge Point roles

 * Nicer syntax for constructing OCPP messages to send or expect

## Other ideas

 * Web interface: click together test: 150 CPs behaving like this, 300 like that, ..., GO!

 * Live demo on the web?


