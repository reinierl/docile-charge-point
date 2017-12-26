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


