(ns le-webform-glue.preload
  {:dev/always true}
  (:require [le-webform-glue.webform-facts-to-le.core]
            [malli.instrument.cljs :as mi]))

(mi/instrument!)