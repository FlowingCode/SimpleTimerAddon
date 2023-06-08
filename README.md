[![Build Status](https://jenkins.flowingcode.com/job/SimpleTimer-14-addon/badge/icon)](https://jenkins.flowingcode.com/job/SimpleTimer-14-addon)

# Simple Timer Addon

Simple Timer for Vaadin 10+ based on https://github.com/annsonn/simple-timer

## Online demo

[Online demo here](http://addonsv14.flowingcode.com/simple-timer)

## Building and running demo

- git clone repository
- mvn clean install jetty:run

To see the demo, navigate to http://localhost:8080/

## Release notes

- **Version 1.0.0** Supports Vaadin 10-13 and Vaadin 14 in compatibility mode
- **Version 2.0.0** Supports Vaadin 14 in npm mode

## Issue tracking

The issues for this add-on are tracked on its github.com page. All bug reports and feature requests are appreciated. 

## Contributions

Contributions are welcome, but there are no guarantees that they are accepted as such. Process for contributing is the following:

- Fork this project
- Create an issue to this project about the contribution (bug or feature) if there is no such issue about it already. Try to keep the scope minimal.
- Develop and test the fix or functionality carefully. Only include minimum amount of code needed to fix the issue.
- Refer to the fixed issue in commit
- Send a pull request for the original project
- Comment on the original issue that you have implemented a fix for it

## License & Author

This add-on is distributed under Apache License 2.0. For license terms, see LICENSE.txt.

Simple Timer Addon is written by Flowing Code S.A.


# Developer Guide

## Using the component

- Basic usage
```
		SimpleTimer timer = new SimpleTimer(new BigDecimal("30"));
		timer.start();
		timer.pause();
		timer.reset();
		timer.setCountUp(true);
		timer.isRunning();
		
```
- Timer Ended listener
```
		timer.addTimerEndEvent(ev-> Notification.show("Timer ended"));
```
