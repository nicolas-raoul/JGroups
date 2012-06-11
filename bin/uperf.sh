# Tests the UNICAST by invoking unicast RPCs between a sender and a receiver.
# Mimicks the DIST mode in Infinispan.
# Can be used as a benchmark tool.

# Author: Bela Ban

#!/bin/bash

JG=$HOME/JGroups

jgroups.sh org.jgroups.tests.perf.UPerf $*