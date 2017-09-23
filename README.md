# docile-charge-point

A scriptable OCPP charge point simulator.

Not as continuously ill-tempered as
[abusive-charge-point](https://github.com/chargegrid/abusive-charge-point), but
it can be mean if you script it to be.

## TODOs

 * allow expecting responses [X]

 * allow responding to incoming reqs [X]

 * Unified form of error collection and handling in the interpreter [X]

 * Add a DSL that needs no for comprehensions (using mutable state a la specs2?)
 
 * Expectations over more than one message (expectAnOf, expectInAnyOrder, expectEventually)
 
 * Result formatting, process return code
 
 * Including DSL for test names?
 
 * Connection based on command-line args, not supplied in script

---

 * Bundle

 * Announce?

---

 * Split core / not so core DSL

 * Allow run-time loading of text files with scripts

 * Nicer asynchronous-but-checkable open, close operations in OCPP library so we
   can have nicer error reporting about these

 * support OCPP 1.6 TLS + Basic Auth
 

 * Make it able to simulate many charge point at once

 * Make it able to take both Central System and Charge Point roles

 * Make it produce TAP output or something that makes it easy to integrate it in
   a CI test

 * Live demo on the web?

