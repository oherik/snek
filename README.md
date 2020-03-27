This aggressive little snake finished second in the Cygni SnakeBot Challenge 2018. The code is written in standard hackathonesque, that is, pretty ugly but working. The snake tries to bite any other snake that comes too close, searches for food if there's something nearby, and otherwise just tries to stall for time by finding the longest possible route.

# Original readme from Cygni
[![Build Status](http://jenkins.snake.cygni.se/buildStatus/icon?job=snake client java)](http://jenkins.snake.cygni.se/job/snake%20client%20java/)

This a Snake Client written in Java 8.

## Requirements

* Java JDK >= 8
* Gradle
* Snake Server (local or remote)

## Installation

A. Clone the repository: `git clone https://github.com/cygni/snakebot-client-java.git`.

B. Open: `<repo>/`

C. Execute: `./gradlew build`

## Usage

To clean and build:
```
> ./gradlew clean build
```

To run your client:
```
> ./gradlew run
```

## Implementation

There is only one class in this project, have a look at SimpleSnakePlayer.java. The main method to start in looks like this:

```java
@Override
public void onMapUpdate(MapUpdateEvent mapUpdateEvent) {
    ansiPrinter.printMap(mapUpdateEvent);

    // Choose action here!
    registerMove(mapUpdateEvent.getGameTick(), SnakeDirection.DOWN);
}
```

For every MapUpdateEvent received your are expected to reply with a SnakeDirection (UP, DOWN, LEFT or RIGHT). 
