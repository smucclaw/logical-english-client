(ns logical-english-client.utils 
  (:require [applied-science.js-interop :as jsi]
            [cljs-bean.core :as bean]
            [tupelo.string :as str]))

(defn replace-strs [str replacements]
  (reduce (fn [str [match replacement]]
            (str/replace str match replacement))
          str replacements))

(defn clj->json [clj]
  (->> clj
       bean/->js
       (jsi/call js/JSON :stringify)))