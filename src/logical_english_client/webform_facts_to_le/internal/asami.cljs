(ns logical-english-client.webform-facts-to-le.internal.asami 
  (:require [applied-science.js-interop :as jsi]
            [asami.core :as d]
            [cljs-bean.core :as bean]
            [malli.core :as m]))

(defn- is-asami-attr? [x]
  (#{:a/owns :a/first :a/rest :db/ident :a/entity :tx-data} x))

;; (defn- is-name-attr? [x]
;;   (#{:name :id} x))

(defn- asami-db->triples [db]
  (d/q '[:find ?e ?a ?v
         :in $ ?is-asami-attr?
         :where
         [?e ?a ?v]
         [(not= ?v :a/nil)]
         [(not= ?v "")]
         (not [(?is-asami-attr? ?a)])]
       db is-asami-attr?))

(def triple
  [:catn [:entity :any] [:attribute keyword?] [:value :any]])

(m/=> clj-data->triples [:=> [:cat :any] [:sequential triple]])
(defn clj-data->triples [clj-data]
  ;; Note that the fixed point iteration defining uri always terminates.
  ;; Suppose not, then by definition, a database exists for every rational
  ;; number x betweeen 0 and 1, which is absurd because machines only have
  ;; finite memory.
  (let [uri (->> (repeatedly rand)
                 (eduction (comp (map (partial str "asami:multi://db"))
                                 (filter d/create-database)))
                 first)
        db-connection (d/connect uri)
        db (do @(d/transact db-connection {:tx-data clj-data})
               (-> db-connection d/db))
        triples (do (jsi/call js/console :log "Dump of Asami DB:\n" (bean/->js (d/export-data db)))
                    (-> db asami-db->triples))]
    (d/delete-database uri)
    (jsi/call js/console :log "Triples from datalog query over Asami DB:\n" (bean/->js triples))
    triples))