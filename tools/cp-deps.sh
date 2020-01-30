#!/bin/bash

core=0.2.44
spec=0.2.176
clj=1.10.1
tgt=target/default/classes
mkdir -p $tgt
for archive in \
    clojure/$clj/clojure-$clj.jar \
    core.specs.alpha/$core/core.specs.alpha-$core.jar \
    spec.alpha/$spec/spec.alpha-$spec.jar; do
    (
      cd $tgt && \
      jar xf ~/.m2/repository/org/clojure/$archive
    )
done
