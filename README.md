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

 * Expectations over more than one message (expectAnyOf, expectInAnyOrder, expectEventually)

 * Extend the DSL with nicer expectation syntax

 * Make it able to simulate multiple charge points at once

 * Make it able to take both Central System and Charge Point roles

 * Nicer syntax for constructing OCPP messages to send or expect

 * support OCPP 1.6 TLS + Basic Auth

 * Nicer asynchronous-but-checkable open, close operations in OCPP library so we
   can have nicer error reporting about these

## Other ideas

 * Web interface: click together test: 150 CPs behaving like this, 300 like that, ..., GO!

 * Live demo on the web?


