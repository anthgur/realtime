# realtime

Live rendering public realtime GTFS data with Clojure(Script). http://realtime-gtfs.herokuapp.com/

I originally created this project to present at
[Polyglot Programming DC](http://www.meetup.com/Polyglot-Programming-DC/events/223063412/). 
It consumes the [Boston MBTA](http://www.mbta.com/rider_tools/developers/)'s
public GTFS realtime data feed using protocol buffers from a Clojure server and pushes 
the updates to ClojureScript clients.

I originally created the client using reagent, but have since reimplemented it using om.next.

As of 3/30/18 it appears the project has suffered from bitrot and does not render anything on the front end.
I may update it in the future but I do not have any plans to fix it at this time.

## Running Locally

- `docker build -t gtfs-realtime .`
- `docker run -p 8080:8080 -e MBTA_PB_URL="https://cdn.mbta.com/realtime/VehiclePositions.pb" gtfs-realtime:latest`
