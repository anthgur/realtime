# realtime

Live rendering public realtime GTFS data with Clojure(Script). http://realtime-gtfs.herokuapp.com/

I originally created this project to present at
[Polyglot Programming DC](http://www.meetup.com/Polyglot-Programming-DC/events/223063412/). 
It consumes the [Boston MBTA](http://www.mbta.com/rider_tools/developers/)'s
public GTFS realtime data feed using protocol buffers from a Clojure server and pushes 
the updates to ClojureScript clients.

I originally created the client using reagent, but have since reimplemented it using om.next.