# docile-charge-point

A scriptable OCPP charge point simulator.

Not as continuously ill-tempered as
[abusive-charge-point](https://github.com/chargegrid/abusive-charge-point), but
it can be mean if you script it to be.

## TODOs for first release

 * allow expecting responses [X]

 * allow responding to incoming reqs [X]

 * Unified form of error collection and handling in the interpreter [X]

 * Expectations over more than one message (expectAnyOf, expectInAnyOrder, expectEventually) [-]

 * Including DSL for test names? [X]

 * Result formatting, process return code [X]

 * Connection based on command-line args, not supplied in script [X]

 * Execute all tests in a certain DIR, or runtime-loading [X]

 * Now still:

     - output formatting /verbosity control

     - split out of RunTest: runtime loading, test running, test formatting

     - doc in README

     - fat jar + runscript?

---

 * Bundle

 * Announce?

---

 * Split core / not so core DSL

 * Expectations over more than one message (expectAnyOf, expectInAnyOrder, expectEventually)

## TODOs after publication

 * support OCPP 1.6 TLS + Basic Auth

 * Make it able to simulate multiple charge points at once

 * Make it able to take both Central System and Charge Point roles

## Wildish dreams

 * Parser from text, enabling REPL, piping from stdin, or executing script files

 * Web interface: click together test: 150 CPs behaving like this, 300 like that, ..., GO!

 * Live demo on the web?

## Difficult ideas

 * Add a DSL that needs no for comprehensions (using mutable state a la specs2? embed Groovy?)

 * Allow run-time loading of text files with scripts

 * Nicer asynchronous-but-checkable open, close operations in OCPP library so we
   can have nicer error reporting about these

