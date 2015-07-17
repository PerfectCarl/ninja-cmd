## Features
  - not requiring maven
  - simple declarative project file
  - new application templates
  - support automatic browser reload via live-reload
  - auto compilation (java and xtend) without needing a jdk
  - support all the maven actions like run and packaging (jar and war)

## TODO
  - [x] log in `ninja run`
  - [x] compile (top level dep only)
  - [~] intellij
  - [~] download transitive sources: https://issues.apache.org/jira/browse/IVY-1003
  - [x] optimize library/ivy use -> needed for offline work
  - review Eclipse model

## Bugs
  - [~] jade
  - [x] properties to the jetty maven task in the pom.xml
     - change assets: reload | -
     - change views : reload | -
     - change code  : reload | reload
     - project.ninja: none   |   !!
  - [x] shutdown
  - [ ] aggregate livereload events
  - [x] secret generation