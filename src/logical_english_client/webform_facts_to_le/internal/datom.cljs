(ns logical-english-client.webform-facts-to-le.internal.datom
  (:require [logical-english-client.utils :as utils]
            [logical-english-client.webform-facts-to-le.internal.date :as date]
            [malli.core :as malli]
            [tupelo.core :refer [it->]]))

(defprotocol LEDatom
  (le-datom->le-str [this]))

(def le-datom
  #(satisfies? LEDatom %))

(defn- transform-str [s]
  (utils/replace-strs
   s
   [[#"[_]|[\n]|[\r]|[\r\n]" " "]
    ["," " COMMA"]
    ["%" " PERCENT"]
    ;; https://stackoverflow.com/a/45616898 
    [#"[a-zA-z] + [^0-9\s.]+|\.(?!\d)" " PERIOD "]
    [#"(\d+\.\d+)\.(\d+)" "$1 PERIOD $2"]
    [#"\s+" " "]]))

(malli/=> ->le-str [:=> [:sequential :any] :string])
(defn- ->le-str [& rest]
  (let [transform-kw-and-str
        #(cond
           (string? %) (transform-str %)
           (keyword? %) (-> % name transform-str)
           :else %)]
    (->> rest
         (eduction (map transform-kw-and-str))
         (apply str))))

(defrecord MembershipDatom [entity list-entity]
  LEDatom
  (le-datom->le-str [_this]
    (->le-str entity " is in " list-entity)))

;; (defrecord BoolAttrDatom [entity attribute bool-value]
;;   LEDatom
;;   (le-datom->le-str [_this] (when bool-value
;;                               (keywords->str entity " " attribute))))

(defrecord DateDatom [entity attribute date-value]
  LEDatom
  (le-datom->le-str [_this]
    (str (->le-str entity "'s " attribute " is ")
         (date/date->le-date-str date-value))))

(defrecord GenericDatom [entity attribute value]
  LEDatom
  (le-datom->le-str [_this]
    (->le-str entity "'s " attribute " is " value)))

(malli/=> le-datoms->le-scenario [:=> [:cat [:sequential le-datom]] :string])
(defn le-datoms->le-scenario [le-datoms]
  (when (not-empty le-datoms)
    (let [end-of-line ".\n"]
      (it-> le-datoms
       (eduction (comp (map le-datom->le-str)
                       (filter some?)
                       (interpose end-of-line))
                 it)
       (apply str it)
       (str it end-of-line)))))