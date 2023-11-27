(ns le-webform-glue.webform-facts-to-le.core
  (:require [applied-science.js-interop :as jsi]
            [cljs-bean.core :as bean]
            [le-webform-glue.webform-facts-to-le.internal.core :as internal]
            [malli.core :as m]))

(m/=> data->le-scenario [:=> [:cat :any] :string])
(defn data->le-scenario [data]
  (if (string? data)
    (->> data (jsi/call js/JSON :parse) recur)

    (let [le-scenario (-> data bean/->clj internal/clj-data->le-scenario)]
      (jsi/call js/console :log "Input json/edn data to Asami DB:\n" data)
      (jsi/call js/console :log "LE scenario:\n" le-scenario)
      le-scenario)))