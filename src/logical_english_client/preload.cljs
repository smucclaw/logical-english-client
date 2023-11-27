(ns logical-english-client.preload
  {:dev/always true}
  (:require [logical-english-client.webform-facts-to-le.core]
            [malli.instrument.cljs :as mi]))

(mi/instrument!)